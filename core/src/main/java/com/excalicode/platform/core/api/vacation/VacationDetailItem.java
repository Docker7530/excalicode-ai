package com.excalicode.platform.core.api.vacation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 休假记录拆解项（AI解析结果） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationDetailItem {

  /** 开始日期 */
  @JsonProperty("startDate")
  private String startDate;

  /** 结束日期 */
  @JsonProperty("endDate")
  private String endDate;

  /** 休假类型 */
  @JsonProperty("vacationType")
  private String vacationType;

  /** 休假天数（包含单位） */
  @JsonProperty("vacationDays")
  private String vacationDays;
}
