package com.excalicode.platform.web;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

/** EXCALICODE AI 平台主应用程序 */
@Slf4j
@SpringBootApplication
@EnableCaching
@ComponentScan(basePackages = {"com.excalicode.platform.web", "com.excalicode.platform.core"})
@MapperScan("com.excalicode.platform.*.mapper")
public class ExcalicodePlatformApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExcalicodePlatformApplication.class, args);
    log.info("\n\n========== EXCALICODE AI平台启动完成 ==========\n");
  }
}
