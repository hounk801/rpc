package com.entity;


import lombok.Data;

@Data
public class ListQueryResult extends RpcResponse {

    protected Integer total;

    protected Integer offset;

    protected Integer limit;
}
