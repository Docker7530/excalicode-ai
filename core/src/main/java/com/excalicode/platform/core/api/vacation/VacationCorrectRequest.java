package com.excalicode.platform.core.api.vacation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 员工休假记录备注修正请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationCorrectRequest {

  /** 需要修正备注的记录列表 */
  private List<VacationRecordRequest> records;
}
