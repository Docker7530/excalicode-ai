package com.excalicode.platform.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 功能类型 DTO
 *
 * 用于前端展示枚举值
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiFunctionTypeDto {

    /**
     * 功能类型代码
     */
    private String code;

    /**
     * 功能描述
     */
    private String description;
}
