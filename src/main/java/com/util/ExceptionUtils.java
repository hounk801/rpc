package com.util;

/**
 * @author hnk
 * @date 2019/6/27
 */
public class ExceptionUtils {
    public static <T> T searchCause(Throwable t, Class<T> cause) {
        return com.util.ExceptionUtils.searchCause(t, cause);
    }
}
