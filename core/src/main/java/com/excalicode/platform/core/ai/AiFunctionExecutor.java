package com.excalicode.platform.core.ai;

import java.util.ArrayList;
import java.util.List;
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
import com.excalicode.platform.common.enums.AiFunctionType;
import com.excalicode.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 统一的 AI 功能执行入口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiFunctionExecutor {

    private final AiFunctionConfigurationService configurationService;

    public Flux<String> streamText(AiFunctionType functionType, String userPrompt) {
        AiFunctionConfiguration config = configurationService.getConfiguration(functionType);
        Prompt prompt = new Prompt(mergeMessages(config, List.of(new UserMessage(userPrompt))));
        return config.chatModel().stream(prompt).map(response -> {
            String chunk = response.getResult().getOutput().getText();
            return chunk != null ? chunk : "";
        });
    }

    public String executeText(AiFunctionType functionType, String userPrompt) {
        AiFunctionConfiguration config = configurationService.getConfiguration(functionType);
        Prompt prompt = new Prompt(mergeMessages(config, List.of(new UserMessage(userPrompt))));
        ChatResponse chatResponse = config.chatModel().call(prompt);
        String text = chatResponse.getResult().getOutput().getText();
        if (text == null) {
            throw new BusinessException(
                    String.format("AI 功能 [%s] 返回空响应", functionType.getDescription()));
        }
        String trimmed = text.trim();
        log.debug("AI 功能 [{}] 文本响应: {}", functionType.getDescription(), trimmed);
        return trimmed;
    }

    public <T> AiFunctionResult<T> executeStructured(AiFunctionType functionType, String userPrompt,
            Class<T> responseType) {
        return executeStructured(functionType, List.of(new UserMessage(userPrompt)), responseType);
    }

    public <T> AiFunctionResult<T> executeStructured(AiFunctionType functionType,
            List<Message> messages, Class<T> responseType) {
        AiFunctionConfiguration config = configurationService.getConfiguration(functionType);
        BeanOutputConverter<T> converter = new BeanOutputConverter<>(responseType);
        String jsonSchema = converter.getJsonSchema();
        Prompt prompt = buildJsonPrompt(config, messages, jsonSchema);
        ChatResponse chatResponse = config.chatModel().call(prompt);
        String raw = chatResponse.getResult().getOutput().getText();
        if (raw == null) {
            throw new BusinessException(
                    String.format("AI 功能 [%s] 返回空响应", functionType.getDescription()));
        }
        log.info("AI 功能 [{}] 原始响应: {}", functionType.getDescription(), raw);
        T value = converter.convert(raw);
        if (value == null) {
            throw new BusinessException(
                    String.format("AI 功能 [%s] 响应解析失败", functionType.getDescription()));
        }
        validateStructuredResult(value);
        return new AiFunctionResult<>(value, raw);
    }

    private Prompt buildJsonPrompt(AiFunctionConfiguration config, List<Message> messages,
            String jsonSchema) {
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder();
        ResponseFormat responseFormat = config.supportsJsonSchema()
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

    public record AiFunctionResult<T>(T value, String rawResponse) {
    }
}
