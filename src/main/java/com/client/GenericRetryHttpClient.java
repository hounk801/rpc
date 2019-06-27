package com.client;

import com.exception.RetryException;
import com.util.ExceptionUtils;
import play.libs.ws.StandaloneWSResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 *
 * @param <UIN> 输入的url参数
 * @param <BIN> 输入的body参数
 * @param <BOUT> 返回的body参数
 */
public abstract class GenericRetryHttpClient<UIN, BIN, BOUT> {

    protected abstract int getMaxRetryTimes();
    protected abstract int getSleepTime(int retries);
    protected abstract <T extends BOUT> T parseResponse(StandaloneWSResponse response, UIN uin, BIN bin, Class<T> cls);
    protected abstract CompletionStage<StandaloneWSResponse> doRequestWithTrace(HttpMethod method, String path, UIN urlParam, BIN bodyParam, int retries) throws Exception;

    protected <T extends BOUT> CompletionStage<CommonClientResult<T>> doRequestWithRetries(
            HttpMethod method,
            String path,
            UIN urlParam,
            BIN bodyParam,
            Class<T> cls) throws Exception {

        CompletionStage<CommonClientResult<T>> f = doRequestWithTrace(method, path, urlParam, bodyParam, cls, 0);
        for (int i = 1; i < getMaxRetryTimes(); i++) {
            final int retries = i;
            f = f.thenApplyAsync(r -> CompletableFuture.completedFuture(r))
                    .exceptionally(t -> {
                        //由RetryRemoteException引起的
                        if (ExceptionUtils.searchCause(t, RetryException.class) != null) {
                            try {
                                Thread.sleep(getSleepTime(retries));
                                CompletionStage<CommonClientResult<T>> retryResult = doRequestWithTrace(method, path, urlParam, bodyParam, cls, retries);
                                return retryResult.toCompletableFuture();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else if (t instanceof RuntimeException) {
                            throw (RuntimeException) t;
                        } else {
                            throw new RuntimeException(t);
                        }
                    }).thenComposeAsync(Function.identity());
        }
        return f;
    }

    protected <T extends BOUT> CompletionStage<CommonClientResult<T>> doRequestWithTrace(
            HttpMethod method,
            String path,
            UIN urlParam,
            BIN bodyParam,
            Class<T> cls,
            int retries) throws Exception {
        return doRequestWithTrace(method, path, urlParam, bodyParam, retries).thenApply(r -> {
            CommonClientResult result = new CommonClientResult();
            result.setResponse(r);
            if (cls != null) {
                result.setCommonResult(parseResponse(r, urlParam, bodyParam, cls));
            }

            return result;
        });
    }

    public <T extends BOUT> CompletionStage<T> getJsonResult(String path, UIN param, Class<T> cls) throws Exception {
        return doRequestWithRetries(HttpMethod.GET, path, param, null, cls).thenApply(r -> r.getCommonResult());
    }

    public CompletionStage<StandaloneWSResponse> get(String path, UIN param) throws Exception {
        return doRequestWithRetries(HttpMethod.GET, path, param, null, null).thenApply(r -> r.getResponse());
    }

    public <T extends BOUT> CompletionStage<T> postJsonResult(String path, BIN bodyParam, UIN param, Class<T> cls) throws Exception {
        return doRequestWithRetries(HttpMethod.POST, path, param, bodyParam, cls).thenApply(r -> r.getCommonResult());
    }

    public CompletionStage<StandaloneWSResponse> post(String path, BIN bodyParam, UIN param) throws Exception {
        return doRequestWithRetries(HttpMethod.POST, path, param, bodyParam, null).thenApply(r -> r.getResponse());
    }

    public <T extends BOUT> CompletionStage<T> postJsonResult(String path, BIN bodyParam, Class<T> cls) throws Exception {
        return doRequestWithRetries(HttpMethod.POST, path, null, bodyParam, cls).thenApply(r -> r.getCommonResult());
    }

    public CompletionStage<StandaloneWSResponse> post(String path, BIN bodyParam) throws Exception {
        return doRequestWithRetries(HttpMethod.POST, path, null, bodyParam, null).thenApply(r -> r.getResponse());
    }
}

