package com.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 */
@Accessors(chain = true)
@Getter
@Setter
@ToString
public class RemoteException extends RuntimeException {
    private int httpCode = 200;
    private String body;
    private String errorCode;
    private String errorMessage;

    public RemoteException(String errorCode, String message, String body) {
        this(errorCode, message, body, null);
    }

    public RemoteException(int code, String body, Throwable t) {
        super(t);
        this.body = body;
        this.httpCode = code;
    }

    public RemoteException(String errorCode, String message, String body, Throwable t) {
        super(message, t);
        this.errorMessage = message;
        this.errorCode = errorCode;
        this.body = body;
    }
}
