package com.excalicode.platform.web;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

/**
 * EXCALICODE AI 平台主应用程序
 */
@Slf4j
@SpringBootApplication
@EnableCaching
@ComponentScan(basePackages = {"com.excalicode.platform.web", "com.excalicode.platform.core",
        "com.excalicode.platform.common"})
@MapperScan("com.excalicode.platform.*.mapper")
public class ExcalicodePlatformApplication {

    /**
     * 应用程序主入口方法 启动Spring Boot应用程序
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ExcalicodePlatformApplication.class, args);
        log.info("EXCALICODE AI平台启动完成！");
    }
}
