package com.excalicode.platform.core.api.cosmic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 功能过程拆解请求 仅承载原始需求描述，避免与后续流程耦合 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessBreakdownRequest {

  /** 需求描述 用户输入的需求详细描述文本 */
  @NotBlank(message = "需求描述不能为空")
  @Size(min = 10, max = 50000, message = "需求描述长度应在10-50000字符之间")
  private String requirementDescription;

  @Positive(message = "期望的功能过程数量需为正整数")
  private Integer expectedProcessCount;
}
