package com.excalicode.platform.core.api.vacation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 员工休假记录请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationRecordRequest {

  /** 身份证号码 */
  private String idCard;

  /** 姓名 */
  private String name;

  /** 一级部门 */
  private String department;

  /** 备注 */
  private String remark;
}
