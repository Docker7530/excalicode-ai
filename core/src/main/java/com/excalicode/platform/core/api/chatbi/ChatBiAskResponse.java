package com.excalicode.platform.core.api.chatbi;

import lombok.Data;

/** ChatBI 提问响应 */
@Data
public class ChatBiAskResponse {

  /** 会话ID */
  private Long sessionId;

  /** 是否需要澄清 */
  private Boolean needClarification;

  /** 澄清问题 */
  private String clarifyingQuestion;

  /** 助手回答 */
  private String answer;

  /** 查询结果（表格），可能为空 */
  private ChatBiQueryResultResponse result;

  /** 仅用于调试展示（管理员可见） */
  private String debugSql;

  /** 仅用于调试/展示（管理员可见） */
  private String debugPlan;
}
