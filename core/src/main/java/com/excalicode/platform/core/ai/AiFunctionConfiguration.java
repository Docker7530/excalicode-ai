package com.excalicode.platform.core.ai;

import com.excalicode.platform.core.entity.AiModel;
import com.excalicode.platform.core.enums.AiFunctionType;
import org.springframework.ai.chat.model.ChatModel;

/** AI 功能执行所需的聚合配置。 */
record AiFunctionConfiguration(
    AiFunctionType functionType,
    String promptCode,
    String systemPrompt,
    ChatModel chatModel,
    boolean supportsJsonSchema,
    AiModel model) {}
