package com.excalicode.platform.core.config;

import jakarta.validation.constraints.NotNull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** 线程池配置。 */
@Configuration
public class ApplicationExecutorConfig {

  private static final int CORE_POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors());
  private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;
  private static final int QUEUE_CAPACITY = 256;

  /** 平台统一线程池，供各业务模块复用。 */
  @Bean(destroyMethod = "shutdown")
  @Primary
  ExecutorService applicationExecutorService() {
    ThreadFactory threadFactory =
        new ThreadFactory() {
          private final AtomicInteger counter = new AtomicInteger(0);

          @Override
          public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("platform-worker-" + counter.incrementAndGet());
            thread.setDaemon(false);
            return thread;
          }
        };

    return new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAX_POOL_SIZE,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(QUEUE_CAPACITY),
        threadFactory,
        new ThreadPoolExecutor.CallerRunsPolicy());
  }
}
