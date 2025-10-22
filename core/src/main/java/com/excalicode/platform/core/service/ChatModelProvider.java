package com.excalicode.platform.core.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import com.excalicode.platform.common.enums.AiFunctionType;
import com.excalicode.platform.core.entity.AiModel;
import com.excalicode.platform.core.entity.AiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ChatModel 提供者
 *
 * 根据功能类型动态获取对应的 ChatModel，实现功能级别的模型切换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatModelProvider {

    private final ChatModel defaultChatModel;
    private final AiFunctionModelMappingService mappingService;
    private final AiModelService aiModelService;
    private final AiProviderService aiProviderService;

    /**
     * ChatModel 缓存 (modelId -> ChatModel)
     */
    private final Map<Long, ChatModel> chatModelCache = new ConcurrentHashMap<>();

    /**
     * 根据功能类型获取 ChatModel
     *
     * @param functionType 功能类型
     * @return ChatModel 实例
     */
    public ChatModel getChatModel(AiFunctionType functionType) {
        if (functionType == null) {
            log.warn("功能类型为空，使用默认 ChatModel");
            return defaultChatModel;
        }

        // 查询功能类型对应的模型ID
        Long modelId = mappingService.getModelIdByFunctionType(functionType);
        if (modelId == null) {
            log.info("功能类型 {} 未配置模型映射，使用默认 ChatModel", functionType.getDescription());
            return defaultChatModel;
        }

        // 从缓存获取或创建 ChatModel
        return chatModelCache.computeIfAbsent(modelId, this::createChatModel);
    }

    /**
     * 动态创建 ChatModel
     *
     * @param modelId 模型ID
     * @return ChatModel 实例
     */
    private ChatModel createChatModel(Long modelId) {
        log.info("创建 ChatModel，模型ID: {}", modelId);

        // 查询模型信息
        AiModel model = aiModelService.getById(modelId);
        if (model == null) {
            log.error("模型ID {} 不存在，使用默认 ChatModel", modelId);
            return defaultChatModel;
        }

        // 查询厂商信息
        AiProvider provider = aiProviderService.getById(model.getProviderId());
        if (provider == null) {
            log.error("厂商ID {} 不存在，使用默认 ChatModel", model.getProviderId());
            return defaultChatModel;
        }

        log.info("使用厂商: {}, 模型: {}, Base URL: {}", provider.getProviderName(), model.getModelName(),
                provider.getBaseUrl());

        // 创建 OpenAI 兼容的 ChatModel
        return createOpenAiCompatibleChatModel(provider, model);
    }

    /**
     * 创建 OpenAI 兼容的 ChatModel
     *
     * @param provider 厂商信息
     * @param model 模型信息
     * @return ChatModel 实例
     */
    private ChatModel createOpenAiCompatibleChatModel(AiProvider provider, AiModel model) {
        // 创建 OpenAiApi (使用 Builder 模式)
        OpenAiApi openAiApi = OpenAiApi.builder().baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey()).build();

        // 创建 OpenAiChatModel (使用 Builder 模式)
        return OpenAiChatModel.builder().openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(model.getModelName()).build())
                .build();
    }

    /**
     * 清除缓存
     *
     * 当模型或厂商配置更新时，需要清除缓存以使新配置生效
     */
    public void clearCache() {
        log.info("清除 ChatModel 缓存");
        chatModelCache.clear();
    }

    /**
     * 判断功能类型是否支持使用 JSON Schema 响应格式
     *
     * @param functionType 功能类型
     * @return true 表示支持, 默认视为支持
     */
    public boolean supportsJsonSchema(AiFunctionType functionType) {
        AiModel model = getConfiguredModel(functionType);
        return model == null || model.getSupportsJsonSchema() == null
                || Boolean.TRUE.equals(model.getSupportsJsonSchema());
    }

    /**
     * 获取功能类型当前配置的模型信息
     *
     * @param functionType 功能类型
     * @return AI 模型, 若未配置则返回 null
     */
    public AiModel getConfiguredModel(AiFunctionType functionType) {
        if (functionType == null) {
            return null;
        }
        Long modelId = mappingService.getModelIdByFunctionType(functionType);
        if (modelId == null) {
            return null;
        }
        return aiModelService.getById(modelId);
    }

}
