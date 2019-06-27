package com.client;


import lombok.Data;
import play.libs.ws.StandaloneWSClient;
import play.libs.ws.StandaloneWSRequest;

import java.io.IOException;

@Data
public class HupoStandaloneWsClient implements StandaloneWSClient {
    private StandaloneWSClient wsClient;

    public HupoStandaloneWsClient(StandaloneWSClient client) {
        this.wsClient = client;
    }

    @Override
    public Object getUnderlying() {
        return wsClient.getUnderlying();
    }

    @Override
    public StandaloneWSRequest url(String url) {
        return null;//new HupoStandaloneWSRequest(wsClient.url(url));
    }

    @Override
    public void close() throws IOException {
        wsClient.close();
    }
}
