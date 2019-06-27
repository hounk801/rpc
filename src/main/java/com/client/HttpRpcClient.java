package com.client;

/**
 * @author hnk
 * @date 2019/6/27
 */

import akka.stream.ActorMaterializer;
import akka.util.ByteString;
import com.annotation.HttpRpcApi;
import com.config.GlobalConfig;
import com.config.RpcConnectionConfig;
import com.entity.RpcRequest;
import com.entity.RpcResponse;
import com.exception.InvalidRpcResponseException;
import com.exception.RpcRequestException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.http.DefaultHttpActorSystem;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.libs.ws.ahc.AhcWSClientConfig;
import play.libs.ws.InMemoryBodyWritable;
import play.libs.ws.StandaloneWSClient;
import play.libs.ws.StandaloneWSRequest;
import play.libs.ws.StandaloneWSResponse;
import scala.Option;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

@Getter
@Setter
public class HttpRpcClient extends GenericRetryHttpClient<Object, RpcRequest, RpcResponse> {
    private final static Logger logger = LoggerFactory.getLogger(HttpRpcClient.class);

    private String host;
    private String version;
    private boolean traceEnable = true;
    private String label;
    private boolean isSecurity = false;
    private StandaloneWSClient wsClient;
    private GlobalConfig config;
    private String source = "Unkown";

    private ObjectMapper mapper = new ObjectMapper();

    public HttpRpcClient(RpcConnectionConfig config, GlobalConfig globalConfig) {
        this(config);

        if (globalConfig != null) {
            this.config = globalConfig;
            this.source = String.format("%s:%s:%s", globalConfig.getClusterName(), globalConfig.getProjectName(), globalConfig.getEnv());
        }
    }

    HttpRpcClient(RpcConnectionConfig config) {
        this.host = config.getHost();

        if (!host.startsWith("http")) {
            throw new IllegalArgumentException("Host应该包含协议: " + host);
        }
        isSecurity = host.startsWith("https://");
        logger.info("Start http rpc client for {}", config.toString());
        wsClient = HttpClientFactory.getHupoWsClient(label, config.getTimeoutMills(), isSecurity, isTraceEnable());

        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public Object call(RpcRequest req, Class<?> interfaceCls, String method) throws Exception {
        Method methodObject = interfaceCls.getMethod(method, req.getClass());
        if (methodObject == null) {
            throw new IllegalArgumentException(interfaceCls.toGenericString() + "不存在方法" + method);
        }

        return call(req, interfaceCls, methodObject);
    }

    public Object call(RpcRequest req, Class<?> interfaceCls, Method method) throws Exception {
        if (isAsyncCall(method)) {
            return callAsync(req, interfaceCls, method);
        } else {
            return callSync(req, interfaceCls, method);
        }
    }

    public RpcResponse callSync(RpcRequest req, Class<?> interfaceCls, Method method) throws Exception {
        //path的class要有annotation
        Annotation annotation = interfaceCls.getAnnotation(HttpRpcApi.class);
        if (annotation == null) {
            throw new IllegalArgumentException(interfaceCls.toGenericString() + "应该有HttpRpcApi annotation");
        }

        String path = ((HttpRpcApi) annotation).path() + ":" + method.getName();

        return doRequestWithRetries(HttpMethod.POST, path, null, req, (Class<? extends RpcResponse>) method.getReturnType())
                .thenApply(r -> r.getCommonResult()).toCompletableFuture().get();
    }

    private CompletionStage<? extends RpcResponse> callAsync(RpcRequest req, Class<?> interfaceCls, Method method) throws Exception {
        //path的class要有annotation
        Annotation annotation = interfaceCls.getAnnotation(HttpRpcApi.class);
        if (annotation == null) {
            throw new IllegalArgumentException(interfaceCls.toGenericString() + "应该有HttpRpcApi annotation");
        }

        String path = ((HttpRpcApi) annotation).path() + ":" + method.getName();

        Class cls = (Class)((ParameterizedType)method.getGenericReturnType()).getActualTypeArguments()[0];
        if (!RpcResponse.class.isAssignableFrom(cls)) {
            throw new IllegalArgumentException(cls.toGenericString() + "应该继承于RpcResponse");
        }

        return doRequestWithRetries(HttpMethod.POST, path, null, req, (Class<? extends RpcResponse>) cls)
                .thenApply(r -> r.getCommonResult());
    }

    public static boolean isAsyncCall(Method method) {
        return CompletableFuture.class.isAssignableFrom(method.getReturnType());
    }

    @Override
    protected <T extends RpcResponse> T parseResponse(StandaloneWSResponse response, Object o, RpcRequest request, Class<T> cls) {
        String body = response.getBody();

        if (response.getStatus() / 100 != 2) {
            InvalidRpcResponseException exception = new InvalidRpcResponseException(null);
            exception.setHttpStatus(response.getStatus());
            exception.setInput(request.toString());
            exception.setOutput(body);

            throw exception;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("body ret: {}", body);
        }

        if (StringUtils.isBlank(body)) {
            return null;
        }

        ApiResponse<T> apiResponse;
        try {
            JsonNode rootNode = mapper.readTree(body);

            apiResponse = new ApiResponse<T>();

            JsonNode dataNode = rootNode.get("data");

            if (!StringUtils.isEmpty(dataNode.toString())) {
                T data = mapper.treeToValue(dataNode, cls);
                apiResponse.setData(data);
            } else {
                apiResponse.setData(null);
            }

            apiResponse.setCode(rootNode.get("code").asText());
            apiResponse.setMessage(rootNode.get("message").asText());

        } catch (Exception e) {
            InvalidRpcResponseException exception = new InvalidRpcResponseException(e);
            exception.setHttpStatus(response.getStatus());
            exception.setInput(request.toString());
            exception.setOutput(body);
            exception.setExpectedResponseType(cls.toGenericString());

            throw exception;
        }

        if (!"0".equals(apiResponse.getCode())) {
            throw new RpcRequestException(response.getStatus(), apiResponse, null);
        }

        return apiResponse.getData();
    }

    private static final int MAX_RETRIES = 3;

    private CompletionStage<StandaloneWSResponse> doPost(String path, RpcRequest bodyParam) throws Exception {
        StandaloneWSRequest request = wsClient.url(String.format("%s/%s/private/rpc-apis/%s", host, version, path));

        ObjectMapper mapper = new ObjectMapper();
        String bodyStr = mapper.writeValueAsString(bodyParam);
        ByteString jsonStr = ByteString.fromString(bodyStr);
        InMemoryBodyWritable body = new InMemoryBodyWritable(jsonStr, "application/json");

        if (this.traceEnable) {
            request.addHeader("use-trace", "true");
        }

        //计算sign
//        String magicNumber = RpcSignUtils.getMagicNumber();
//        request.addHeader(RpcSignUtils.MAGIC_NUMBER, magicNumber);
//        request.addHeader(RpcSignUtils.SIGN_HEADER, RpcSignUtils.sign(bodyStr, magicNumber).getSign());
//        request.addHeader(RpcSignUtils.SOURCE, source);

        if (logger.isDebugEnabled()) {
            logger.debug("Start RPC request {}, {}", path, bodyStr);
        }

        return (CompletionStage<StandaloneWSResponse>) request.post(body);
    }

    @Override
    protected int getMaxRetryTimes() {
        return MAX_RETRIES;
    }

    @Override
    protected int getSleepTime(int retries) {
        return (retries + 1) * 100;
    }

    @Override
    protected CompletionStage<StandaloneWSResponse> doRequestWithTrace(HttpMethod method, String path, Object urlParam, RpcRequest bodyParam, int retries) throws Exception {
        return doPost(path, bodyParam);
    }

    public static class HttpClientFactory {
        private static final Logger logger = LoggerFactory.getLogger(HttpClientFactory.class);
        private static ActorMaterializer materializer = ActorMaterializer.apply(Option.apply(null), Option.apply(null), DefaultHttpActorSystem.defaultSystem());
        private static Map<String, StandaloneWSClient> clientMap = new ConcurrentHashMap<>();
        private static ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("http-client-rpc-%d").build();

        public static synchronized StandaloneWSClient getHupoWsClient(
                String label,
                long timeOutMills,
                boolean isSecurity,
                boolean traceEnable) throws RuntimeException {

            StandaloneWSClient client = clientMap.get(label + timeOutMills);

//            HttpTracingConfig tracingConfig = null;
//            if (traceEnable) {
//                tracingConfig = new HttpTracingConfig(label);
//            }
            if (client == null) {
                AhcWSClientConfig wsConfig =null;
                client = HupoWsClient.create(
                        wsConfig,
                        null,
                        label,
                        materializer,
                        null,
                        factory
                );

                clientMap.put(label + timeOutMills, client);
            }

            return client;
        }

        public static void shutDown() throws IOException {
            for (StandaloneWSClient client : clientMap.values()) {
                client.close();
            }
        }
    }
}
