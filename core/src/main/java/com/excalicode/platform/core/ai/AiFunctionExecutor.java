package com.excalicode.platform.core.ai;

import com.excalicode.platform.core.enums.AiFunctionType;
import com.excalicode.platform.core.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

/** 统一的 AI 功能执行入口。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiFunctionExecutor {

  private final AiFunctionConfigurationService configurationService;

  /**
   * 流式执行 AI 功能，返回文本流。
   *
   * @param functionType 功能类型
   * @param userPrompt 用户输入的提示
   * @return 文本流
   */
  public Flux<String> streamText(AiFunctionType functionType, String userPrompt) {
    AiFunctionConfiguration config = configurationService.getConfiguration(functionType);
    Prompt prompt = new Prompt(mergeMessages(config, List.of(new UserMessage(userPrompt))));
    return config.chatModel().stream(prompt)
        .map(
            response -> {
              String chunk = response.getResult().getOutput().getText();
              return chunk != null ? chunk : "";
            })
        .filter(chunk -> !chunk.isEmpty())
        .doOnSubscribe(subscription -> log.info("开始流式推送 AI 功能 [{}]", functionType.getDescription()))
        .doOnNext(chunk -> log.info("AI 功能 [{}] 流式片段: {}", functionType.getDescription(), chunk))
        .doOnComplete(() -> log.info("AI 功能 [{}] 流式完成", functionType.getDescription()))
        .doOnError(error -> log.error("AI 功能 [{}] 流式失败", functionType.getDescription(), error));
  }

  /**
   * 执行 AI 功能，返回文本响应。
   *
   * @param functionType 功能类型
   * @param userPrompt 用户输入的提示
   * @return 文本响应
   */
  public String executeText(AiFunctionType functionType, String userPrompt) {
    AiFunctionConfiguration config = configurationService.getConfiguration(functionType);
    Prompt prompt = new Prompt(mergeMessages(config, List.of(new UserMessage(userPrompt))));
    ChatResponse chatResponse = config.chatModel().call(prompt);
    String text = chatResponse.getResult().getOutput().getText();
    if (text == null) {
      throw new BusinessException(String.format("AI 功能 [%s] 返回空响应", functionType.getDescription()));
    }
    String trimmed = text.trim();
    log.info("AI 功能 [{}] 文本响应: {}", functionType.getDescription(), trimmed);
    return trimmed;
  }

  /**
   * 执行 AI 功能，返回结构化响应。
   *
   * @param functionType 功能类型
   * @param userPrompt 用户输入的提示
   * @param responseType 响应类型
   * @return 结构化响应
   */
  public <T> T executeStructured(
      AiFunctionType functionType, String userPrompt, Class<T> responseType) {
    return executeStructured(functionType, List.of(new UserMessage(userPrompt)), responseType);
  }

  /**
   * 执行 AI 功能，返回结构化响应。
   *
   * @param functionType 功能类型
   * @param messages 消息列表
   * @param responseType 响应类型
   * @return 结构化响应
   */
  public <T> T executeStructured(
      AiFunctionType functionType, List<Message> messages, Class<T> responseType) {
    AiFunctionConfiguration config = configurationService.getConfiguration(functionType);
    BeanOutputConverter<T> converter = new BeanOutputConverter<>(responseType);
    String jsonSchema = converter.getJsonSchema();
    Prompt prompt = buildJsonPrompt(config, messages, jsonSchema);
    ChatResponse chatResponse = config.chatModel().call(prompt);
    String raw = chatResponse.getResult().getOutput().getText();
    if (raw == null) {
      throw new BusinessException(String.format("AI 功能 [%s] 返回空响应", functionType.getDescription()));
    }
    log.info("AI 功能 [{}] 原始响应: {}", functionType.getDescription(), raw);
    T value = converter.convert(raw);
    if (value == null) {
      throw new BusinessException(
          String.format("AI 功能 [%s] 响应解析失败", functionType.getDescription()));
    }
    validateStructuredResult(value);
    return value;
  }

  /**
   * 这里是为了适配 deepseek 等不支持直接 JSON Schema 的模型。
   *
   * <p>openai 规范模型支持直接 JSON Schema；真的非常 nice。
   *
   * <p>详情：https://api-docs.deepseek.com/zh-cn/guides/json_mode
   */
  private Prompt buildJsonPrompt(
      AiFunctionConfiguration config, List<Message> messages, String jsonSchema) {
    OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder();
    ResponseFormat responseFormat =
        config.supportsJsonSchema()
            ? new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, jsonSchema)
            : new ResponseFormat(ResponseFormat.Type.JSON_OBJECT, null);
    optionsBuilder.responseFormat(responseFormat);
    return new Prompt(mergeMessages(config, messages), optionsBuilder.build());
  }

  private List<Message> mergeMessages(AiFunctionConfiguration config, List<Message> messages) {
    List<Message> finalMessages = new ArrayList<>();
    finalMessages.add(new SystemMessage(config.systemPrompt()));
    if (!CollectionUtils.isEmpty(messages)) {
      finalMessages.addAll(messages);
    }
    return finalMessages;
  }

  private void validateStructuredResult(Object value) {
    if (value instanceof Iterable<?> iterable && !iterable.iterator().hasNext()) {
      throw new BusinessException("AI 返回结构为空");
    }
    if (value instanceof String str && !StringUtils.hasText(str)) {
      throw new BusinessException("AI 返回结构为空字符串");
    }
  }
}
