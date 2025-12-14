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

  /** 核心线程数，取 CPU 核心数与 4 的最大值。 */
  private static final int CORE_POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors());

  /** 任务队列容量。 */
  private static final int QUEUE_CAPACITY = 256;

  /** 最大线程数，为核心线程数的两倍。 */
  private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;

  /**
   * 创建平台统一线程池 {@link ExecutorService}。
   *
   * <p>线程池特点：
   *
   * <ul>
   *   <li>核心线程数：{@link #CORE_POOL_SIZE}
   *   <li>最大线程数：{@link #MAX_POOL_SIZE}
   *   <li>队列容量：{@link #QUEUE_CAPACITY}
   *   <li>线程存活时间：非核心线程空闲 60 秒回收
   *   <li>线程工厂：自定义线程名 {@code platform-worker-<序号>}，非守护线程
   *   <li>拒绝策略：{@link ThreadPoolExecutor.CallerRunsPolicy}
   * </ul>
   *
   * @return 平台统一线程池 {@link ExecutorService} Bean
   */
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
