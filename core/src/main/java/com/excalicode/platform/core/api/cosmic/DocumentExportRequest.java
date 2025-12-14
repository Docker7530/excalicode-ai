package com.excalicode.platform.core.api.cosmic;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 需求文档导出请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentExportRequest {

  /** 导出文档内容 */
  @NotBlank(message = "请提供要导出的需求文档内容")
  private String overrideDocumentContent;
}
