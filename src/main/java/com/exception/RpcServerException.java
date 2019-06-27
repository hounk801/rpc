package com.exception;

import lombok.Data;

@Data
public class RpcServerException extends RuntimeException {
    protected String source;
    protected String input;
    protected String output;
    protected String path;
    protected String method;

    public RpcServerException(Exception e) {
        super(e);
    }

    @Override
    public String getMessage() {
        return this.toString() + "," + super.getMessage();
    }
}
