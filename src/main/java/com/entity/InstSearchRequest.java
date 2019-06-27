package com.entity;

import lombok.Data;

@Data
public class InstSearchRequest extends ListQueryParam {

    public InstSearchRequest() {
        this.setPageInfo(0, Integer.MAX_VALUE);
    }

    private String merchantType;

    /**
     * 关键字搜索
     */
    private String name;
}
