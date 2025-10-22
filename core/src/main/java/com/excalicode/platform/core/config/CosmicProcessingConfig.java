package com.excalicode.platform.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * COSMIC处理配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cosmic.processing")
public class CosmicProcessingConfig {

    /**
     * 阶段2并发线程数
     */
    private int concurrency = 3;

    /**
     * AI调用失败重试次数
     */
    private int maxRetries = 3;

    /**
     * 重试延迟(毫秒)
     */
    private long retryDelayMillis = 5000;

    /**
     * AI调用超时时间(秒)
     */
    private int timeoutSeconds = 300;

    /**
     * 重复项修复最大轮数
     */
    private int maxFixRounds = 3;

}
