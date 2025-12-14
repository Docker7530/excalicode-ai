package com.excalicode.platform.core.ai;

import com.excalicode.platform.core.config.CacheConfig;
import com.excalicode.platform.core.entity.AiModel;
import com.excalicode.platform.core.entity.AiPromptTemplate;
import com.excalicode.platform.core.entity.AiProvider;
import com.excalicode.platform.core.enums.AiFunctionType;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.service.entity.AiFunctionModelMappingService;
import com.excalicode.platform.core.service.entity.AiFunctionPromptMappingService;
import com.excalicode.platform.core.service.entity.AiModelService;
import com.excalicode.platform.core.service.entity.AiPromptTemplateService;
import com.excalicode.platform.core.service.entity.AiProviderService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 聚合 AI 功能执行所需的提示词与模型配置，并提供缓存。 */
@Slf4j
@Service
@RequiredArgsConstructor
class AiFunctionConfigurationService {

  private final AiFunctionPromptMappingService aiFunctionPromptMappingService;
  private final AiPromptTemplateService aiPromptTemplateService;
  private final AiFunctionModelMappingService functionModelMappingService;
  private final AiModelService aiModelService;
  private final AiProviderService aiProviderService;
  private final ChatModel defaultChatModel;

  /** 根据功能类型组合出完整执行配置。 */
  @Cacheable(value = CacheConfig.AI_FUNCTION_CONFIGS_CACHE, key = "#functionType.code")
  public AiFunctionConfiguration getConfiguration(AiFunctionType functionType) {
    Objects.requireNonNull(functionType, "functionType 不能为空");
    String functionCode = functionType.getCode();
    String promptCode = aiFunctionPromptMappingService.getPromptCodeByFunctionCode(functionCode);
    if (!StringUtils.hasText(promptCode)) {
      throw new BusinessException(String.format("功能 [%s] 未配置提示词映射", functionType.getDescription()));
    }

    AiPromptTemplate promptTemplate = aiPromptTemplateService.getByCode(promptCode);
    if (promptTemplate == null) {
      throw new BusinessException(String.format("提示词模板 [%s] 不存在", promptCode));
    }

    String promptContent = promptTemplate.getContent();
    if (!StringUtils.hasText(promptContent)) {
      throw new BusinessException(String.format("提示词模板 [%s] 内容为空", promptCode));
    }

    Long modelId = functionModelMappingService.getModelIdByFunctionType(functionType);
    ChatModel chatModel = defaultChatModel;
    AiModel model = null;
    boolean supportsJsonSchema = true;

    if (modelId != null) {
      model = aiModelService.getById(modelId);
      if (model == null) {
        log.warn("功能 {} 映射的模型 {} 不存在，回退到默认 ChatModel", functionCode, modelId);
      } else {
        AiProvider provider = aiProviderService.getById(model.getProviderId());
        if (provider == null) {
          log.warn("模型 {} 映射的厂商 {} 不存在，回退到默认 ChatModel", modelId, model.getProviderId());
        } else {
          chatModel = createChatModel(provider, model);
          supportsJsonSchema =
              model.getSupportsJsonSchema() == null || model.getSupportsJsonSchema();
        }
      }
    }

    return new AiFunctionConfiguration(
        functionType, promptCode, promptContent, chatModel, supportsJsonSchema, model);
  }

  private ChatModel createChatModel(AiProvider provider, AiModel model) {
    if (!StringUtils.hasText(provider.getBaseUrl())
        || !StringUtils.hasText(provider.getApiKey())
        || !StringUtils.hasText(model.getModelName())) {
      log.warn("厂商或模型配置缺失，使用默认 ChatModel。provider={}, model={}", provider.getId(), model.getId());
      return defaultChatModel;
    }

    OpenAiApi openAiApi =
        OpenAiApi.builder().baseUrl(provider.getBaseUrl()).apiKey(provider.getApiKey()).build();

    return OpenAiChatModel.builder()
        .openAiApi(openAiApi)
        .defaultOptions(OpenAiChatOptions.builder().model(model.getModelName()).build())
        .build();
  }
}
