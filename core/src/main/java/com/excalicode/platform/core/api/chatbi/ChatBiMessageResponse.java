package com.excalicode.platform.core.api.chatbi;

import java.time.LocalDateTime;
import lombok.Data;

/** ChatBI 消息 */
@Data
public class ChatBiMessageResponse {

  private Long id;
  private String role;
  private String content;
  private LocalDateTime createdTime;

  private ChatBiQueryResultResponse result;
}
