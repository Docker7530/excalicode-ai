package com.excalicode.platform.core.api.cosmic;

import com.excalicode.platform.core.model.cosmic.CosmicProcess;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 时序图生成请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SequenceDiagramRequest {

  /**
   * 功能过程与子过程描述。
   *
   * <p>当 processes 为空时，可以使用 text 直接生成。
   */
  @Valid
  @JsonPropertyDescription("COSMIC过程列表（兼容旧逻辑）；为空时可使用 text 生成")
  private List<CosmicProcess> processes;

  /** 用户输入的描述文本（支持多行） */
  @JsonPropertyDescription("用户输入的描述文本（支持多行）；可直接粘贴 Mermaid sequenceDiagram")
  private String text;
}
