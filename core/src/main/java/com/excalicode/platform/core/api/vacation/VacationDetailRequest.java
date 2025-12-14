package com.excalicode.platform.core.api.vacation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 员工休假详细记录请求（最终休假数据表） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationDetailRequest {

  /** 身份证号码 */
  private String idCard;

  /** 姓名 */
  private String name;

  /** 开始日期 */
  private String startDate;

  /** 结束日期 */
  private String endDate;

  /** 开始时间（保持为空） */
  private String startTime;

  /** 结束时间（保持为空） */
  private String endTime;

  /** 休假天数 */
  private String vacationDays;

  /** 休假类型 */
  private String vacationType;

  /** 年休假归属年份（保持为空） */
  private String annualLeaveYear;

  /** 子女姓名（保持为空） */
  private String childName;

  /** 备注（保持为空） */
  private String remark;

  /** 一级部门 */
  private String department;
}
