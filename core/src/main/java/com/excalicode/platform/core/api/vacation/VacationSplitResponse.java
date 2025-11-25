package com.excalicode.platform.core.api.vacation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 员工休假记录拆分结果响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationSplitResponse {

  /** 筛选后的休假记录列表 */
  private List<VacationRecordRequest> records;

  /** 总记录数 */
  private Integer totalCount;

  /** 有效记录数(备注不为空的记录) */
  private Integer validCount;
}
