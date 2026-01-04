package com.excalicode.platform.core.api.chatbi;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** ChatBI 提问请求 */
@Data
public class ChatBiAskRequest {

  /** 会话ID（为空表示新建会话） */
  private Long sessionId;

  /** 用户问题 */
  @NotBlank(message = "问题不能为空")
  private String question;
}
