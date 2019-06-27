package com.entity;

import lombok.Data;

@Data
public class ListQueryParam extends RpcRequest {

    private Integer offset;

    private Integer limit;

    public void setPageInfo(Integer offset, Integer limit) {
        this.offset = offset;
        this.limit = limit;

        if (this.offset == null) {
            this.offset = 0;
        }
        if (this.limit == null) {
            this.limit = Integer.MAX_VALUE;
        }
    }

}
