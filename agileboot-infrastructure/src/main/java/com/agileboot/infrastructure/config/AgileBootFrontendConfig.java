package com.agileboot.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AgileBoot application.yaml中的prefix = "agileboot.frontend"配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "agileboot.frontend")
public class AgileBootFrontendConfig {

    /**
     * 前端访问地址
     */
    private String url;
}

