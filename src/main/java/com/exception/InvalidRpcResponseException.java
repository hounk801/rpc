package com.exception;

import lombok.Data;

@Data
public class InvalidRpcResponseException extends RpcServerException {
    private int httpStatus;
    private String expectedResponseType;

    public InvalidRpcResponseException(Exception e) {
        super(e);
    }
}
