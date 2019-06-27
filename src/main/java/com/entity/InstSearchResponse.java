package com.entity;


import lombok.Data;

import java.util.List;

/**
 * 机构列表
 */
@Data
public class InstSearchResponse extends ListQueryResult {

    public InstSearchResponse() {
    }

    public InstSearchResponse(List<Object> data, Integer total, Integer offset, Integer limit) {
        this.institutions = data;
        this.total = total;
        this.offset = offset;
        this.limit = limit;
    }

    private List<Object> institutions;
}
