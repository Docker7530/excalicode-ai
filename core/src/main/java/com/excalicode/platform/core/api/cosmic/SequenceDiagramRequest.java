package com.excalicode.platform.core.api.cosmic;

import com.excalicode.platform.core.model.cosmic.CosmicProcess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 时序图生成请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SequenceDiagramRequest {

  /** 功能过程与子过程描述 */
  @NotEmpty(message = "COSMIC过程列表不能为空")
  @Valid
  private List<CosmicProcess> processes;
}
