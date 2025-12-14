package com.excalicode.platform.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** WebMvc 异步配置类。 */
@Configuration
public class WebMvcAsyncConfig implements WebMvcConfigurer {

  private static final int CORE_POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors());
  private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;
  private static final int QUEUE_CAPACITY = 256;

  @Bean("mvcTaskExecutor")
  AsyncTaskExecutor mvcTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(CORE_POOL_SIZE);
    executor.setMaxPoolSize(MAX_POOL_SIZE);
    executor.setQueueCapacity(QUEUE_CAPACITY);
    executor.setThreadNamePrefix("mvc-async-");
    executor.initialize();
    return executor;
  }

  @Override
  public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    configurer.setTaskExecutor(mvcTaskExecutor());
  }
}
