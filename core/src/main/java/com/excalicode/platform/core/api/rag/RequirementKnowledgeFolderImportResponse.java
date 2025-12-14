package com.excalicode.platform.core.api.rag;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/** 文件夹批量导入的响应结构，整体统计与文件结果 */
@Value
@Builder
public class RequirementKnowledgeFolderImportResponse {
  String folderName;
  int totalFiles;
  int eligibleFiles;
  int ingestedFiles;
  int skippedFiles;
  @Builder.Default List<FileImportResult> fileResults = List.of();

  @Value
  @Builder
  public static class FileImportResult {
    String path;
    boolean ingested;
    String documentId;
    String reason;
  }
}
