package com.excalicode.platform.core.api.chatbi;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** ChatBI 会话详情 */
@Data
public class ChatBiSessionDetailResponse {

  private Long id;
  private String title;
  private LocalDateTime lastActiveTime;
  private List<ChatBiMessageResponse> messages;
}
