package com.excalicode.platform.core.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** AI 功能类型枚举。 */
@Getter
@AllArgsConstructor
public enum AiFunctionType {
  COSMIC_PM("COSMIC_PM", "资深产品经理"),
  COSMIC_FUNCTIONAL("COSMIC_FUNCTIONAL", "功能过程拆解"),
  COSMIC_ANALYSIS("COSMIC_ANALYSIS", "COSMIC 拆分"),
  COSMIC_PRD("COSMIC_PRD", "PRD 文档生成"),
  COSMIC_SEQUENCE_DIAGRAM("COSMIC_SEQUENCE_DIAGRAM", "时序图生成"),
  COSMIC_ESTIMATE("COSMIC_ESTIMATE", "锐评大师"),

  QINSHI_ATTENDANCE("QINSHI_ATTENDANCE", "勤时考勤数据处理");

  private final String code;
  private final String description;

  private static final Map<String, AiFunctionType> CODE_MAP =
      Arrays.stream(values())
          .collect(Collectors.toMap(AiFunctionType::getCode, Function.identity()));

  /**
   * 根据 code 查找枚举。
   *
   * @param code 功能类型代码
   * @return 对应的枚举值, 找不到返回 Optional.empty()
   */
  public static Optional<AiFunctionType> fromCode(String code) {
    return Optional.ofNullable(CODE_MAP.get(code));
  }
}
