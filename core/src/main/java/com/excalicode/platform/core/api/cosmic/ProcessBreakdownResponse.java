package com.excalicode.platform.core.api.cosmic;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 功能过程拆解结果 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessBreakdownResponse {

  /** 功能过程列表 AI拆解生成的功能过程,用户可以对此进行编辑 */
  private List<FunctionalProcess> functionalProcesses;
}
