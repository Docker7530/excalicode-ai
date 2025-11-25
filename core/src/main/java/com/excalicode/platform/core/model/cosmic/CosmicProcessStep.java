package com.excalicode.platform.core.model.cosmic;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** COSMIC 过程步骤 阶段1：只有 subProcessDesc 和 dataMovementType 阶段2：增加 dataGroup 和 dataAttributes */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CosmicProcessStep {

  /** 子过程描述 */
  @NotBlank(message = "子过程描述不能为空")
  @JsonPropertyDescription("详细描述该功能过程下的具体操作步骤")
  private String subProcessDesc;

  /** 数据移动类型 */
  @NotBlank(message = "数据移动类型不能为空")
  @JsonPropertyDescription("COSMIC分析中的数据移动分类：E(输入)、R(读取)、W(写入)、X(输出)")
  private String dataMovementType;

  /** 数据组（阶段2填充） */
  @JsonPropertyDescription("该过程操作的数据组或数据实体")
  private String dataGroup;

  /** 数据属性（阶段2填充） */
  @JsonPropertyDescription("涉及的具体数据字段或属性列表")
  private String dataAttributes;
}
