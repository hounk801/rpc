package com.client;

import com.config.GlobalConfig;
import com.config.RpcConnectionConfig;
import com.entity.RpcRequest;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author hnk
 * @date 2019/6/27
 */
public class HttpRcpClientBuilder {
    private RpcConnectionConfig config;
    private GlobalConfig globalConfig;
    private HttpRpcClient client;

    /**
     * 在用的时候再初始化
     * @return
     */
    private HttpRpcClient getClient() {
        if (client == null) {
            client = new HttpRpcClient(config, globalConfig);
            return client;
        } else {
            return client;
        }
    }

    private InvocationHandler invocationHandler = new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args.length != 1 && RpcRequest.class.isAssignableFrom(args[0].getClass())) {
                throw new IllegalArgumentException("args数量为1且需要继承自RpcRequest");
            }
            try {
                return getClient().call((RpcRequest) args[0], method.getDeclaringClass(), method);
            } catch (InvocationTargetException e) {
                if (e.getCause() != null) {
                    throw e.getCause();
                } else {
                    throw e;
                }
            }
        }
    };

    public HttpRcpClientBuilder(RpcConnectionConfig config, GlobalConfig globalConfig) {
        this.config = config;
        this.globalConfig = globalConfig;
    }

    public <T> T getInterface(Class<? extends T> cls) {
        Class<? extends T>[] interfaceClass = new Class[1];
        interfaceClass[0] = cls;
        return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), interfaceClass, invocationHandler);
    }
}

