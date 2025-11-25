package com.excalicode.platform.core.api.cosmic;

import com.excalicode.platform.core.model.cosmic.CosmicProcess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 功能过程表导出请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTableExportRequest {

  /** COSMIC 过程列表 */
  @NotEmpty(message = "COSMIC过程列表不能为空")
  @Valid
  private List<CosmicProcess> processes;
}
