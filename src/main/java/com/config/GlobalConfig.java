package com.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.Serializable;

/**
 * @author hnk
 * @date 2019/6/27
 */
@Accessors(chain = true)
@NoArgsConstructor
@Getter
@Setter
@ToString
@Configuration
@PropertySource(value="classpath:application.properties")
public class GlobalConfig implements Serializable {
    @org.springframework.beans.factory.annotation.Value("${env}")
    private String env;

    @org.springframework.beans.factory.annotation.Value("${project.name}")
    private String projectName;

    @org.springframework.beans.factory.annotation.Value("${project.cluster}")
    private String clusterName;
}
