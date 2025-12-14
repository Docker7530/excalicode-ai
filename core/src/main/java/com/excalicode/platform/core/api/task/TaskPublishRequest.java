package com.excalicode.platform.core.api.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/** 任务发布请求 */
@Data
public class TaskPublishRequest {

  /** 批次/大任务标题 */
  @NotBlank(message = "批次标题不能为空")
  private String batchTitle;

  /** 批次说明 */
  private String batchDescription;

  /** 发布的任务列表 */
  @NotEmpty(message = "任务列表不能为空")
  @Valid
  private List<TaskPublishItem> tasks;

  /** 任务条目 */
  @Data
  public static class TaskPublishItem {

    /** Excel 行号 */
    private int rowIndex;

    /** 任务标题 */
    @NotBlank(message = "任务标题不能为空")
    private String title;

    /** 任务描述 */
    @NotBlank(message = "任务描述不能为空")
    private String description;

    /** 工作量(人天) */
    @NotNull(message = "工作量不能为空")
    private BigDecimal workloadManDay;

    /** 执行人ID */
    @NotNull(message = "执行人不能为空")
    private Long assigneeId;
  }
}
