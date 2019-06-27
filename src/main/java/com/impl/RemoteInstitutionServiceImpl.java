package com.impl;

import com.api.RemoteInstitutionService;
import com.client.HttpRcpClientBuilder;
import com.config.GlobalConfig;
import com.config.RpcConnectionConfig;
import com.entity.InstSearchRequest;
import com.entity.InstSearchResponse;

public class RemoteInstitutionServiceImpl implements RemoteInstitutionService {

    private RemoteInstitutionService remoteInstitutionService;

    public RemoteInstitutionServiceImpl(String host, GlobalConfig globalConfig) {
        RpcConnectionConfig connectionConfig = new RpcConnectionConfig();
        connectionConfig.setHost(host);
        remoteInstitutionService = new HttpRcpClientBuilder(connectionConfig, globalConfig).getInterface(RemoteInstitutionService.class);
    }

    @Override
    public InstSearchResponse queryInst(InstSearchRequest request) throws Exception {
        return remoteInstitutionService.queryInst(request);
    }

}
