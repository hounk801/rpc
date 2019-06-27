package com.exception;

import com.client.ApiResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RpcRequestException extends RpcServerException {
    private int httpStatus;
    private String code;
    private String message;

    public RpcRequestException(int statusCode, ApiResponse response, Exception e) {
        super(e);
        this.code = response.getCode();
        this.message = response.getMessage();
        this.httpStatus = statusCode;
    }
}

