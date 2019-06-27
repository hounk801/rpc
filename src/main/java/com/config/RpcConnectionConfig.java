package com.config;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
@ToString
public class RpcConnectionConfig {

    private String host;
    private long timeoutMills = TimeUnit.SECONDS.toMillis(5);

}
