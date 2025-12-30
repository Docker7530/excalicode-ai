package com.excalicode.platform.core.service;

import com.excalicode.platform.core.api.rag.RequirementKnowledgeImportError;
import com.excalicode.platform.core.api.rag.RequirementKnowledgeImportResponse;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeDocument;
import com.excalicode.platform.core.service.entity.RequirementKnowledgeEntryService;
import com.google.common.base.Splitter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 需求知识库 Excel 批量导入服务（仅入库到数据库，不自动向量化） */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementKnowledgeImportService {

  private static final int MAX_ROWS = 2000;
  private static final int MAX_ERRORS = 20;

  private static final Splitter TAG_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private static final String HEADER_TITLE = "标题";
  private static final String HEADER_TAGS = "标签";
  private static final String HEADER_CONTENT = "正文";

  private final RequirementKnowledgeEntryService requirementKnowledgeEntryService;

  public RequirementKnowledgeImportResponse importExcel(MultipartFile file) {
    DataFormatter formatter = new DataFormatter();
    if (file == null || file.isEmpty()) {
      throw new BusinessException("请上传 Excel 文件");
    }
    String filename = file.getOriginalFilename();
    if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
      throw new BusinessException("仅支持 .xlsx 或 .xls 格式的 Excel 文件");
    }

    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(inputStream)) {
      Sheet sheet = workbook.getSheetAt(0);
      if (sheet == null) {
        throw new BusinessException("Excel 缺少有效的工作表");
      }

      int lastRowNum = sheet.getLastRowNum();
      if (lastRowNum + 1 > MAX_ROWS) {
        throw new BusinessException("Excel 行数过多，最多支持 " + MAX_ROWS + " 行");
      }

      Row firstRow = sheet.getRow(0);
      if (firstRow == null) {
        throw new BusinessException("Excel 缺少有效数据");
      }

      ColumnIndex index = resolveColumnIndex(firstRow, formatter);
      int startRowIndex = index.hasHeader ? 1 : 0;

      int totalRows = 0;
      int successCount = 0;
      int failedCount = 0;
      int skippedCount = 0;
      List<RequirementKnowledgeImportError> errors = new ArrayList<>();

      for (int rowIndex = startRowIndex; rowIndex <= lastRowNum; rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
          skippedCount++;
          continue;
        }

        String title = getCellString(row.getCell(index.titleCol), formatter);
        String tagsRaw = getCellString(row.getCell(index.tagsCol), formatter);
        String content = getCellString(row.getCell(index.contentCol), formatter);

        if (!StringUtils.hasText(title)
            && !StringUtils.hasText(tagsRaw)
            && !StringUtils.hasText(content)) {
          skippedCount++;
          continue;
        }

        totalRows++;
        int displayRowIndex = rowIndex + 1;

        if (!StringUtils.hasText(title)) {
          failedCount++;
          addError(errors, displayRowIndex, "标题不能为空");
          continue;
        }
        if (!StringUtils.hasText(content)) {
          failedCount++;
          addError(errors, displayRowIndex, "正文不能为空");
          continue;
        }

        try {
          RequirementKnowledgeDocument document =
              RequirementKnowledgeDocument.builder()
                  .title(title.trim())
                  .content(content.trim())
                  .tags(parseTags(tagsRaw))
                  .build();
          requirementKnowledgeEntryService.saveDraft(document);
          successCount++;
        } catch (BusinessException ex) {
          failedCount++;
          addError(errors, displayRowIndex, ex.getMessage());
        } catch (Exception ex) {
          failedCount++;
          log.error("导入知识条目失败: row={}", displayRowIndex, ex);
          addError(errors, displayRowIndex, "系统异常: " + ex.getMessage());
        }
      }

      if (totalRows == 0 && skippedCount > 0) {
        throw new BusinessException("Excel 未包含有效数据");
      }
      return RequirementKnowledgeImportResponse.builder()
          .totalRows(totalRows)
          .successCount(successCount)
          .failedCount(failedCount)
          .skippedCount(skippedCount)
          .errors(errors)
          .build();
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("读取/解析 Excel 失败", ex);
      throw new BusinessException("解析 Excel 失败: " + ex.getMessage());
    }
  }

  private void addError(
      List<RequirementKnowledgeImportError> errors, int rowIndex, String message) {
    if (errors.size() >= MAX_ERRORS) {
      return;
    }
    errors.add(
        RequirementKnowledgeImportError.builder().rowIndex(rowIndex).message(message).build());
  }

  private List<String> parseTags(String raw) {
    if (!StringUtils.hasText(raw)) {
      return List.of();
    }
    String normalized =
        raw.replace("\r", "")
            .replace("\n", ",")
            .replace("，", ",")
            .replace("；", ",")
            .replace(";", ",");

    LinkedHashSet<String> unique = new LinkedHashSet<>();
    for (String tag : TAG_SPLITTER.split(normalized)) {
      unique.add(tag);
    }
    return new ArrayList<>(unique);
  }

  private String getCellString(Cell cell, DataFormatter formatter) {
    if (cell == null) {
      return "";
    }
    return formatter.formatCellValue(cell);
  }

  private ColumnIndex resolveColumnIndex(Row firstRow, DataFormatter formatter) {
    ColumnIndex headerIndex = tryResolveHeader(firstRow, formatter);
    if (headerIndex != null) {
      return headerIndex;
    }
    return new ColumnIndex(false, 0, 1, 2);
  }

  private ColumnIndex tryResolveHeader(Row row, DataFormatter formatter) {
    if (row == null) {
      return null;
    }
    int titleCol = -1;
    int tagsCol = -1;
    int contentCol = -1;

    int lastCellNum = row.getLastCellNum();
    int last = lastCellNum > 0 ? Math.min(lastCellNum, 50) : 0;
    for (int i = 0; i < last; i++) {
      String value = formatter.formatCellValue(row.getCell(i));
      if (!StringUtils.hasText(value)) {
        continue;
      }
      String normalized = value.trim().toLowerCase(Locale.ROOT);
      if (titleCol < 0
          && (normalized.equals(HEADER_TITLE)
              || normalized.contains("title")
              || normalized.contains("标题"))) {
        titleCol = i;
      }
      if (tagsCol < 0
          && (normalized.equals(HEADER_TAGS)
              || normalized.contains("tags")
              || normalized.contains("标签"))) {
        tagsCol = i;
      }
      if (contentCol < 0
          && (normalized.equals(HEADER_CONTENT)
              || normalized.contains("content")
              || normalized.contains("正文"))) {
        contentCol = i;
      }
    }

    if (titleCol >= 0 && contentCol >= 0) {
      if (tagsCol < 0) {
        int candidate = titleCol + 1;
        if (candidate == contentCol) {
          candidate = contentCol - 1;
        }
        tagsCol = Math.max(0, candidate);
      }
      return new ColumnIndex(true, titleCol, tagsCol, contentCol);
    }
    return null;
  }

  private static class ColumnIndex {
    final boolean hasHeader;
    final int titleCol;
    final int tagsCol;
    final int contentCol;

    ColumnIndex(boolean hasHeader, int titleCol, int tagsCol, int contentCol) {
      this.hasHeader = hasHeader;
      this.titleCol = titleCol;
      this.tagsCol = tagsCol;
      this.contentCol = contentCol;
    }
  }
}
