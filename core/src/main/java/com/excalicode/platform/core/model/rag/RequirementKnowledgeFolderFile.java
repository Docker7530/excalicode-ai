package com.excalicode.platform.core.model.rag;

import lombok.Builder;
import lombok.Value;

/** 批量导入时上传的单个文件 */
@Value
@Builder
public class RequirementKnowledgeFolderFile {
  String fileName;
  String relativePath;
  String content;
}
