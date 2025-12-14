package com.excalicode.platform.core.api.cosmic;

import com.excalicode.platform.core.model.cosmic.CosmicProcess;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Data;

/** COSMIC 分析 AI 响应包装类 */
@Data
public class CosmicProcessesResponse {

  @Valid private List<CosmicProcess> processes;
}
