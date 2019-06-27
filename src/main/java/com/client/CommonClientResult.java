package com.client;

import lombok.Data;
import play.libs.ws.StandaloneWSResponse;

/**
 * @author hnk
 * @date 2019/6/27
 */
@Data
public class CommonClientResult<T> {
    private StandaloneWSResponse response;
    private T commonResult;
}