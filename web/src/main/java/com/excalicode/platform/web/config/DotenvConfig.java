package com.excalicode.platform.web.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/** 环境变量配置类，在应用启动时从 .env 文件加载环境变量。 */
@Slf4j
@Configuration
public class DotenvConfig {

  @PostConstruct
  public void loadEnv() {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    dotenv
        .entries()
        .forEach(
            entry -> {
              String key = entry.getKey();
              String value = entry.getValue();
              if (System.getenv(key) == null) {
                System.setProperty(key, value);
              }
            });
  }
}
