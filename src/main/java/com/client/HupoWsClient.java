package com.client;


import akka.stream.Materializer;
import com.http.HttpTracingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.libs.ws.ahc.AhcWSClientConfig;
import play.api.libs.ws.ahc.cache.AhcHttpCache;
import play.api.libs.ws.ahc.cache.CachingAsyncHttpClient;
import play.libs.ws.StandaloneWSClient;
import play.libs.ws.ahc.StandaloneAhcWSClient;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;

import java.util.concurrent.ThreadFactory;

/**
 * Created by kunjiang on 17/7/15.
 */
public class HupoWsClient {
    private final static Logger logger = LoggerFactory.getLogger(HupoWsClient.class);

    public static StandaloneWSClient create(AhcWSClientConfig ahcWSClientConfig, AhcHttpCache cache, String metricTag, Materializer materializer) {
        return create(ahcWSClientConfig, cache, metricTag, materializer, null, null);
    }

    public static StandaloneWSClient create(AhcWSClientConfig ahcWSClientConfig,
                                            AhcHttpCache cache,
                                            String metricTag,
                                            Materializer materializer,
                                            HttpTracingConfig tracingConfig,
                                            ThreadFactory threadFactory) {
        AsyncHttpClient ahcClient = null;
        AsyncHttpClient defaultAsyncHttpClient = null;//AsyncClientFactory.getClient(ahcWSClientConfig, threadFactory, metricTag);
        if (cache != null) {
            logger.info("HttpClient use cache");
            ahcClient = new CachingAsyncHttpClient(defaultAsyncHttpClient, cache);
        } else {
            ahcClient = defaultAsyncHttpClient;
        }

        if (metricTag != null && !metricTag.isEmpty()) {
            logger.info("HttpClient use monitoring");
//            ahcClient = new MonitoringAsyncHttpClient(ahcClient, metricTag);
        }

        // tracing
        if (tracingConfig != null) {
//            HttpTracing httpTracing = HttpTracingFactory.getTracing(tracingConfig);
//            ahcClient = new TracingAsyncHttpClient(ahcClient, httpTracing);
        }

        return new HupoStandaloneWsClient(new StandaloneAhcWSClient(ahcClient, materializer));
    }
}


