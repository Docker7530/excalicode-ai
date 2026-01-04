package com.excalicode.platform.core.api.chatbi;

import java.util.List;
import lombok.Data;

/** ChatBI 查询结果 表格 */
@Data
public class ChatBiQueryResultResponse {

  /** 列名 按顺序 */
  private List<String> columns;

  /** 行数据每行按 columns 对应顺序 */
  private List<List<Object>> rows;
}
