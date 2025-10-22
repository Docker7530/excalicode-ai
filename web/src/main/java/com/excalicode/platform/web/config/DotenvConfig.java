package com.excalicode.platform.web.config;

import org.springframework.context.annotation.Configuration;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 环境变量配置类
 *
 * 在应用启动时从 .env 文件加载环境变量 这样可以避免将敏感信息（如数据库密码、API密钥等）提交到 git
 */
@Slf4j
@Configuration
public class DotenvConfig {

    @PostConstruct
    public void loadEnv() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            String key = entry.getKey();
            String value = entry.getValue();
            // 只有当系统环境变量中不存在时才设置（系统环境变量优先级更高）
            if (System.getenv(key) == null) {
                System.setProperty(key, value);
            }
        });

    }

}
