package com.excalicode.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 功能类型枚举。
 */
@Getter
@AllArgsConstructor
public enum AiFunctionType {

    COSMIC_REQUIREMENT_ENHANCE("COSMIC_REQUIREMENT_ENHANCE", "需求扩写美化"),
    COSMIC_FUNCTIONAL_BREAKDOWN("COSMIC_FUNCTIONAL_BREAKDOWN", "功能过程拆解"),
    COSMIC_ANALYSIS("COSMIC_ANALYSIS", "COSMIC 拆分——单阶段流程"),
    COSMIC_ANALYSIS_PHASE1("COSMIC_ANALYSIS_PHASE1", "COSMIC 分析——阶段1"),
    COSMIC_ANALYSIS_PHASE2("COSMIC_ANALYSIS_PHASE2", "COSMIC 分析——阶段2"),
    COSMIC_FIX_DUPLICATES("COSMIC_FIX_DUPLICATES", "数据属性重复项修复"),
    COSMIC_PRD_GENERATION("COSMIC_PRD_GENERATION", "PRD 文档生成"),

    QINSHI_DATA_PROCESSING("QINSHI_DATA_PROCESSING", "勤时数据处理");

    private static final Map<String, AiFunctionType> CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(AiFunctionType::getCode, Function.identity()));

    private final String code;
    private final String description;

    /**
     * 根据 code 查找枚举。
     *
     * @param code 功能类型代码
     * @return 对应的枚举值, 找不到返回 null
     */
    public static Optional<AiFunctionType> fromCode(String code) {
        return Optional.ofNullable(CODE_MAP.get(code));
    }

}
