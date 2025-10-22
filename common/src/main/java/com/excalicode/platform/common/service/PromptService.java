package com.excalicode.platform.common.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.excalicode.platform.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

/**
 * 通用提示词管理服务
 */
@Slf4j
@Service
public class PromptService {

    private static final String PROMPT_BASE_PATH = "prompts/";
    private static final String PROMPT_FILE_EXTENSION = ".md";

    /**
     * 根据提示词名称加载提示词内容
     *
     * @param promptName 提示词文件名（不含扩展名）
     * @return 提示词内容
     */
    @Cacheable(value = "prompts", key = "#promptName")
    public String loadPrompt(String promptName) {
        try {
            String filePath = PROMPT_BASE_PATH + promptName + PROMPT_FILE_EXTENSION;
            ClassPathResource resource = new ClassPathResource(filePath);
            if (!resource.exists()) {
                throw new IllegalArgumentException("提示词文件不存在: " + filePath);
            }

            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("读取提示词文件失败: {}", promptName, e);
            throw new BusinessException("读取提示词文件失败: " + promptName, e);
        }
    }

}
