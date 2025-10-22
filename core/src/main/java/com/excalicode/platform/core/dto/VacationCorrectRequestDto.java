package com.excalicode.platform.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 员工休假记录备注修正请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationCorrectRequestDto {

    /**
     * 需要修正备注的记录列表
     */
    private List<VacationRecordDto> records;
}
