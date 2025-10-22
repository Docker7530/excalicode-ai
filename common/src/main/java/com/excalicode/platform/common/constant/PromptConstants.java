package com.excalicode.platform.common.constant;

/**
 * 提示词常量定义
 */
public final class PromptConstants {

    /**
     * 拆解功能过程
     */
    public static final String COSMIC_FUNCTIONAL_PROCESS = "cosmic-functional-process";

    /**
     * 子过程描述
     */
    public static final String COSMIC_SUBPROCEDURE = "cosmic-subprocedure";

    /**
     * 子过程描述-阶段1(生成基础字段)
     */
    public static final String COSMIC_SUBPROCEDURE_PHASE1 = "cosmic-subprocedure-phase1";

    /**
     * 子过程描述-阶段2(生成数据组和数据属性)
     */
    public static final String COSMIC_SUBPROCEDURE_PHASE2 = "cosmic-subprocedure-phase2";

    /**
     * 修复重复项
     */
    public static final String COSMIC_FIX_DUPLICATES = "cosmic-fix-duplicates";

    /**
     * 生成 PRD
     */
    public static final String COSMIC_PRD = "cosmic-prd";

    /**
     * 需求扩写美化
     */
    public static final String REQUIREMENT_ENHANCE = "cosmic-requirement-enhance";

    /**
     * 数据规整 - 备注修正
     */
    public static final String QINSHI_DATA_PROCESSING = "qinshi-data-processing";

    private PromptConstants() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

}
