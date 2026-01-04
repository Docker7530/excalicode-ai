package com.excalicode.platform.core.api.chatbi;

import java.time.LocalDateTime;
import lombok.Data;

/** ChatBI 会话摘要 */
@Data
public class ChatBiSessionSummaryResponse {

  private Long id;
  private String title;
  private LocalDateTime lastActiveTime;
}
