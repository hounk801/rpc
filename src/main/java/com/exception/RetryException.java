package com.exception;

import com.exception.RemoteException;
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
public class RetryException extends RemoteException {
    public RetryException(String message) {
        super("", message, "");
    }

    public RetryException(String message, Throwable t) {
        super("", message, "", t);
    }
}

