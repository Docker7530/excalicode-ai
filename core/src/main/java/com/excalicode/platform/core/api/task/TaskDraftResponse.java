package com.excalicode.platform.core.api.task;

import java.math.BigDecimal;
import lombok.Data;

/** Excel 导入后返回的任务草稿信息 */
@Data
public class TaskDraftResponse {

  /** Excel 行号（从 2 开始计算，便于提示） */
  private int rowIndex;

  /** 任务标题 */
  private String title;

  /** 任务描述 */
  private String description;

  /** 计入工作量(人天) */
  private BigDecimal workloadManDay;
}
