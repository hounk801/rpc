package com.api;

import com.annotation.HttpRpcApi;
import com.entity.InstSearchRequest;
import com.entity.InstSearchResponse;

@HttpRpcApi(path = "institution-search")
public interface RemoteInstitutionService {
    /**
     * 查询机构
     *
     * @param request
     * @return
     * @throws Exception
     */
    InstSearchResponse queryInst(InstSearchRequest request) throws Exception;

}
