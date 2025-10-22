package com.excalicode.platform.common.enums;

import lombok.Getter;

/**
 * AI 功能类型枚举
 *
 * 定义系统中所有需要调用 AI 的功能类型，用于功能与 AI 模型的映射
 */
@Getter
public enum AiFunctionType {

    /**
     * 需求扩写美化
     */
    REQUIREMENT_ENHANCE("REQUIREMENT_ENHANCE", "需求扩写美化"),

    /**
     * 功能过程拆解
     */
    FUNCTIONAL_PROCESS_BREAKDOWN("FUNCTIONAL_PROCESS_BREAKDOWN", "功能过程拆解"),

    /**
     * COSMIC 分析 - 阶段1 (生成基础字段)
     */
    COSMIC_ANALYSIS_PHASE1("COSMIC_ANALYSIS_PHASE1", "COSMIC分析-阶段1"),

    /**
     * COSMIC 分析 - 阶段2 (生成数据组和数据属性)
     */
    COSMIC_ANALYSIS_PHASE2("COSMIC_ANALYSIS_PHASE2", "COSMIC分析-阶段2"),

    /**
     * COSMIC 分析 - 单阶段流程 (旧版处理逻辑)
     */
    COSMIC_ANALYSIS_V1("COSMIC_ANALYSIS_V1", "COSMIC分析-单阶段流程"),

    /**
     * COSMIC 重复项修复
     */
    COSMIC_FIX_DUPLICATES("COSMIC_FIX_DUPLICATES", "COSMIC重复项修复"),

    /**
     * PRD 文档生成
     */
    PRD_GENERATION("PRD_GENERATION", "PRD文档生成");

    /**
     * 功能类型代码 (存储在数据库中)
     */
    private final String code;

    /**
     * 功能描述
     */
    private final String description;

    AiFunctionType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据 code 查找枚举
     *
     * @param code 功能类型代码
     * @return 对应的枚举值, 找不到返回 null
     */
    public static AiFunctionType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AiFunctionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
