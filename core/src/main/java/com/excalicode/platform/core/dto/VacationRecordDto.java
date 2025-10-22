package com.excalicode.platform.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工休假记录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationRecordDto {

    /**
     * 身份证号码
     */
    private String idCard;

    /**
     * 姓名
     */
    private String name;

    /**
     * 一级部门
     */
    private String department;

    /**
     * 备注
     */
    private String remark;

    /**
     * 修正后的备注
     */
    private String correctedRemark;
}
