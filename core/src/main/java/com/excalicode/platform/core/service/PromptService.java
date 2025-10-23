package com.excalicode.platform.core.service;

import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.excalicode.platform.common.enums.AiFunctionType;
import com.excalicode.platform.common.exception.BusinessException;
import com.excalicode.platform.core.entity.PromptTemplate;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 提示词管理服务
 *
 * 基于数据库的动态提示词查询和缓存管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final FunctionPromptMappingService functionPromptMappingService;
    private final PromptTemplateService promptTemplateService;

    /**
     * 提示词内容缓存
     *
     * Key: functionCode (String) Value: prompt content (String)
     *
     * 缓存策略: - 最大容量: 100 (足够容纳所有功能的提示词) - 过期时间: 1小时 (避免数据库更新后缓存不生效) - 并发安全: Caffeine 内部使用
     * ConcurrentHashMap，线程安全
     */
    private final Cache<String, String> promptCache = Caffeine.newBuilder().maximumSize(100)
            .expireAfterWrite(Duration.ofHours(1)).recordStats() // 记录缓存统计信息
            .build();

    /**
     * 根据 AI 功能类型获取提示词内容 (推荐使用)
     *
     * @param functionType AI功能类型枚举
     * @return 提示词内容
     * @throws BusinessException 如果提示词不存在或查询失败
     */
    public String getPrompt(AiFunctionType functionType) {
        if (functionType == null) {
            throw new BusinessException("功能类型不能为空");
        }
        return getPrompt(functionType.getCode());
    }

    /**
     * 根据功能代码获取提示词内容
     *
     * @param functionCode 功能代码 (对应 AiFunctionType.code)
     * @return 提示词内容
     * @throws BusinessException 如果提示词不存在或查询失败
     */
    public String getPrompt(String functionCode) {
        if (!StringUtils.hasText(functionCode)) {
            throw new BusinessException("功能代码不能为空");
        }

        // 使用 Caffeine 的 get 方法，自动处理缓存未命中的情况
        try {
            return promptCache.get(functionCode, this::loadPromptFromDatabase);
        } catch (Exception e) {
            // Caffeine 会包装异常，需要解包
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            log.error("获取提示词失败: functionCode={}", functionCode, e);
            throw new BusinessException(String.format("获取提示词失败: %s", functionCode), e);
        }
    }

    /**
     * 从数据库加载提示词 (私有方法，由缓存调用)
     *
     * @param functionCode 功能代码
     * @return 提示词内容
     * @throws BusinessException 如果提示词不存在或查询失败
     */
    private String loadPromptFromDatabase(String functionCode) {
        log.info("从数据库加载提示词: functionCode={}", functionCode);

        // Step 1: 查询功能对应的提示词代码
        String promptCode = functionPromptMappingService.getPromptCodeByFunctionCode(functionCode);
        if (!StringUtils.hasText(promptCode)) {
            throw new BusinessException(String
                    .format("功能 [%s] 未配置提示词映射，请在 function_prompt_mapping 表中配置", functionCode));
        }

        // Step 2: 查询提示词模板
        PromptTemplate promptTemplate = promptTemplateService.getByCode(promptCode);
        if (promptTemplate == null) {
            throw new BusinessException(
                    String.format("提示词模板 [%s] 不存在，请在 prompt_template 表中配置", promptCode));
        }

        // Step 3: 获取提示词内容
        String content = promptTemplate.getContent();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(String.format("提示词模板 [%s] 内容为空", promptCode));
        }

        log.info("成功加载提示词: functionCode={}, promptCode={}, contentLength={}", functionCode,
                promptCode, content.length());

        return content;
    }

    /**
     * 清除指定功能的提示词缓存
     *
     * 应在以下场景调用: 1. 更新 PromptTemplate 内容时 2. 修改 FunctionPromptMapping 映射关系时
     *
     * @param functionCode 功能代码
     */
    public void evictCache(String functionCode) {
        if (StringUtils.hasText(functionCode)) {
            promptCache.invalidate(functionCode);
            log.info("已清除提示词缓存: functionCode={}", functionCode);
        }
    }

    /**
     * 根据提示词代码清除缓存
     *
     * 应在更新 PromptTemplate 时调用，清除所有使用该模板的功能缓存
     *
     * @param promptCode 提示词代码
     */
    public void evictCacheByPromptCode(String promptCode) {
        if (!StringUtils.hasText(promptCode)) {
            return;
        }

        // 清除所有使用该 promptCode 的 functionCode 的缓存
        // 由于我们不知道哪些 functionCode 使用了这个 promptCode，所以直接清空所有缓存
        log.info("提示词模板 [{}] 已更新，清除所有提示词缓存", promptCode);
        evictAllCache();
    }

    /**
     * 清除所有提示词缓存
     *
     * 应在以下场景调用: 1. 批量更新提示词模板时 2. 批量修改映射关系时
     */
    public void evictAllCache() {
        promptCache.invalidateAll();
        log.info("已清除所有提示词缓存");
    }

    /**
     * 获取缓存统计信息 (用于监控和调试)
     *
     * @return 缓存统计信息字符串
     */
    public String getCacheStats() {
        var stats = promptCache.stats();
        return String.format("PromptCache统计 - 命中率: %.2f%%, 请求次数: %d, 命中次数: %d, 未命中次数: %d, 驱逐次数: %d",
                stats.hitRate() * 100, stats.requestCount(), stats.hitCount(), stats.missCount(),
                stats.evictionCount());
    }
}
