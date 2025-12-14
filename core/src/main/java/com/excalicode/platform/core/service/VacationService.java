package com.excalicode.platform.core.service;

import com.excalicode.platform.core.ai.AiFunctionExecutor;
import com.excalicode.platform.core.api.vacation.VacationDetailItem;
import com.excalicode.platform.core.api.vacation.VacationDetailRequest;
import com.excalicode.platform.core.api.vacation.VacationRecordRequest;
import com.excalicode.platform.core.enums.AiFunctionType;
import com.excalicode.platform.core.exception.BusinessException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 员工休假记录服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VacationService {

  private static final Map<String, String> COLUMN_MAPPING =
      Map.of("身份证号码", "idCard", "姓名", "name", "一级部门", "department", "备注", "remark");
  private static final Set<String> FIXED_ONE_DAY_VACATION_TYPES = Set.of("迟到", "迟到早退", "未刷卡");
  private static final BigDecimal HOURS_PER_DAY = new BigDecimal("8");
  private static final String[] VACATION_TABLE_HEADERS = {
    "序号", "身份证号码", "姓名", "开始日期", "结束日期", "开始时间", "结束时间", "休假天数", "休假类型", "年休假归属年份", "子女姓名", "备注",
    "一级部门"
  };
  private static final int[] VACATION_TABLE_COLUMN_WIDTHS = {
    8, 20, 12, 15, 15, 12, 12, 12, 12, 15, 12, 20, 20
  };
  private static final String VACATION_TABLE_SHEET_NAME = "休假数据表";

  private final AiFunctionExecutor aiFunctionExecutor;
  private final ExecutorService applicationExecutorService;

  /**
   * 一站式处理上传的Excel，直接生成休假数据表Excel文件
   *
   * @param file 上传的Excel
   * @return 休假数据表Excel二进制内容
   */
  public byte[] processVacationExcel(MultipartFile file) {
    List<VacationRecordRequest> validRecords = parseValidVacationRecords(file);
    if (validRecords == null || validRecords.isEmpty()) {
      throw new BusinessException("Excel文件中没有需要处理的备注记录");
    }

    List<VacationDetailRequest> detailRecords = buildVacationDetails(validRecords);
    if (detailRecords == null || detailRecords.isEmpty()) {
      throw new BusinessException("AI解析未生成有效的休假数据，请检查备注内容");
    }

    return buildVacationDetailWorkbook(detailRecords);
  }

  private List<VacationRecordRequest> parseValidVacationRecords(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("上传的文件不能为空");
    }

    String filename = file.getOriginalFilename();
    if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
      throw new BusinessException("文件格式不正确，请上传Excel文件(.xlsx或.xls)");
    }

    try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);

      if (sheet.getPhysicalNumberOfRows() < 2) {
        throw new BusinessException("Excel文件中没有数据");
      }

      // 读取表头,识别目标列的索引
      Row headerRow = sheet.getRow(0);
      Map<String, Integer> columnIndexMap = parseHeader(headerRow);

      // 验证必需列是否存在
      validateRequiredColumns(columnIndexMap);

      // 解析数据行
      int totalCount = 0;
      List<VacationRecordRequest> validRecords = new ArrayList<>();

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }

        totalCount++;
        VacationRecordRequest record = parseRow(row, columnIndexMap);
        if (record.getRemark() != null && !record.getRemark().trim().isEmpty()) {
          validRecords.add(record);
        }
      }

      log.info("解析Excel完成,总记录数:{},有效记录数:{}", totalCount, validRecords.size());
      return validRecords;

    } catch (BusinessException e) {
      throw e;
    } catch (IOException e) {
      log.error("读取Excel文件失败", e);
      throw new BusinessException("读取Excel文件失败: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("解析Excel文件时发生未知错误", e);
      throw new BusinessException("解析Excel文件失败: " + e.getMessage(), e);
    }
  }

  private Map<String, Integer> parseHeader(Row headerRow) {
    Map<String, Integer> columnIndexMap = new HashMap<>();

    if (headerRow == null) {
      throw new BusinessException("Excel表头不能为空");
    }

    for (Cell cell : headerRow) {
      String headerValue = getCellValueAsString(cell).trim();
      if (COLUMN_MAPPING.containsKey(headerValue)) {
        columnIndexMap.put(COLUMN_MAPPING.get(headerValue), cell.getColumnIndex());
      }
    }

    return columnIndexMap;
  }

  private void validateRequiredColumns(Map<String, Integer> columnIndexMap) {
    List<String> missingColumns = new ArrayList<>();

    for (String requiredField : COLUMN_MAPPING.values()) {
      if (!columnIndexMap.containsKey(requiredField)) {
        // 找到对应的中文列名
        for (Map.Entry<String, String> entry : COLUMN_MAPPING.entrySet()) {
          if (entry.getValue().equals(requiredField)) {
            missingColumns.add(entry.getKey());
            break;
          }
        }
      }
    }

    if (!missingColumns.isEmpty()) {
      throw new BusinessException("Excel缺少必需的列: " + String.join(", ", missingColumns));
    }
  }

  private VacationRecordRequest parseRow(Row row, Map<String, Integer> columnIndexMap) {
    return VacationRecordRequest.builder()
        .idCard(getCellValue(row, columnIndexMap.get("idCard")))
        .name(getCellValue(row, columnIndexMap.get("name")))
        .department(getCellValue(row, columnIndexMap.get("department")))
        .remark(getCellValue(row, columnIndexMap.get("remark")))
        .build();
  }

  private String getCellValue(Row row, Integer columnIndex) {
    if (columnIndex == null) {
      return "";
    }

    Cell cell = row.getCell(columnIndex);
    return getCellValueAsString(cell);
  }

  private String getCellValueAsString(Cell cell) {
    if (cell == null) {
      return "";
    }

    switch (cell.getCellType()) {
      case STRING:
        return cell.getStringCellValue();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          return cell.getDateCellValue().toString();
        }
        // 对于数字类型,如果是整数则不显示小数点
        double numericValue = cell.getNumericCellValue();
        if (numericValue == Math.floor(numericValue)) {
          return String.valueOf((long) numericValue);
        }
        return String.valueOf(numericValue);
      case BOOLEAN:
        return String.valueOf(cell.getBooleanCellValue());
      case FORMULA:
        try {
          return cell.getStringCellValue();
        } catch (Exception e) {
          return String.valueOf(cell.getNumericCellValue());
        }
      case BLANK:
      default:
        return "";
    }
  }

  private byte[] buildVacationDetailWorkbook(List<VacationDetailRequest> detailRecords) {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(VACATION_TABLE_SHEET_NAME);
      createHeaderRow(sheet);
      populateDetailRows(sheet, detailRecords);
      adjustColumnWidths(sheet);

      workbook.write(outputStream);
      log.info("休假数据表Excel生成完成，共{}条记录", detailRecords.size());
      return outputStream.toByteArray();
    } catch (IOException e) {
      log.error("休假数据表Excel生成失败", e);
      throw new BusinessException("生成休假数据表Excel失败: " + e.getMessage(), e);
    }
  }

  private void createHeaderRow(Sheet sheet) {
    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < VACATION_TABLE_HEADERS.length; i++) {
      headerRow.createCell(i).setCellValue(VACATION_TABLE_HEADERS[i]);
    }
  }

  private void populateDetailRows(Sheet sheet, List<VacationDetailRequest> detailRecords) {
    for (int i = 0; i < detailRecords.size(); i++) {
      VacationDetailRequest detail = detailRecords.get(i);
      Row row = sheet.createRow(i + 1);
      int column = 0;

      row.createCell(column++).setCellValue((double) i + 1);
      row.createCell(column++).setCellValue(defaultString(detail.getIdCard()));
      row.createCell(column++).setCellValue(defaultString(detail.getName()));
      row.createCell(column++).setCellValue(defaultString(detail.getStartDate()));
      row.createCell(column++).setCellValue(defaultString(detail.getEndDate()));
      row.createCell(column++).setCellValue(defaultString(detail.getStartTime()));
      row.createCell(column++).setCellValue(defaultString(detail.getEndTime()));
      row.createCell(column++).setCellValue(defaultString(detail.getVacationDays()));
      row.createCell(column++).setCellValue(defaultString(detail.getVacationType()));
      row.createCell(column++).setCellValue(defaultString(detail.getAnnualLeaveYear()));
      row.createCell(column++).setCellValue(defaultString(detail.getChildName()));
      row.createCell(column++).setCellValue(defaultString(detail.getRemark()));
      row.createCell(column).setCellValue(defaultString(detail.getDepartment()));
    }
  }

  private void adjustColumnWidths(Sheet sheet) {
    for (int i = 0; i < VACATION_TABLE_COLUMN_WIDTHS.length; i++) {
      sheet.setColumnWidth(i, VACATION_TABLE_COLUMN_WIDTHS[i] * 256);
    }
  }

  private String defaultString(String value) {
    return value == null ? "" : value;
  }

  private List<VacationDetailRequest> buildVacationDetails(List<VacationRecordRequest> records) {
    if (records == null || records.isEmpty()) {
      return new ArrayList<>();
    }

    log.info("开始生成休假数据表，共{}条记录", records.size());

    List<CompletableFuture<List<VacationDetailRequest>>> futures = new ArrayList<>(records.size());
    for (VacationRecordRequest record : records) {
      futures.add(
          CompletableFuture.supplyAsync(
              () -> buildDetailsForRecord(record), applicationExecutorService));
    }

    List<VacationDetailRequest> detailRecords = new ArrayList<>();
    for (CompletableFuture<List<VacationDetailRequest>> future : futures) {
      List<VacationDetailRequest> partial = future.join();
      if (partial != null && !partial.isEmpty()) {
        detailRecords.addAll(partial);
      }
    }

    log.info("休假数据表生成完成，共生成{}条详细记录", detailRecords.size());
    return detailRecords;
  }

  private List<VacationDetailRequest> buildDetailsForRecord(VacationRecordRequest record) {
    List<VacationDetailRequest> details = new ArrayList<>();
    if (record == null || record.getRemark() == null || record.getRemark().trim().isEmpty()) {
      return details;
    }

    try {
      String correctedRemark = correctRemark(record.getRemark());
      details.addAll(convertToDetailRequests(record, correctedRemark));
    } catch (Exception ex) {
      log.error("处理休假记录失败：{} - {}", record.getName(), record.getIdCard(), ex);
    }

    return details;
  }

  private String correctRemark(String remark) {
    if (remark == null || remark.trim().isEmpty()) {
      return "";
    }

    try {
      String correctedRemark = invokeAiCorrection(remark);
      log.info("备注修正 - 原始: {}, 修正后: {}", remark, correctedRemark);
      return correctedRemark;
    } catch (NonTransientAiException ex) {
      log.error("备注修正失败: {}", remark, ex);
      return remark;
    } catch (Exception e) {
      log.error("备注修正失败: {}", remark, e);
      return remark;
    }
  }

  private String invokeAiCorrection(String remark) {
    return aiFunctionExecutor.executeText(AiFunctionType.QINSHI_ATTENDANCE, remark);
  }

  private List<VacationDetailRequest> convertToDetailRequests(
      VacationRecordRequest record, String correctedRemark) {
    List<VacationDetailRequest> detailRecords = new ArrayList<>();
    if (correctedRemark == null || correctedRemark.trim().isEmpty()) {
      log.warn("记录缺少修正备注，跳过：{} - {}", record.getName(), record.getIdCard());
      return detailRecords;
    }

    List<VacationDetailItem> items = parseVacationDetails(correctedRemark);
    for (VacationDetailItem item : items) {
      String vacationType = item.getVacationType();
      String finalVacationDays = shouldUseFixedOneDay(vacationType) ? "1" : item.getVacationDays();

      detailRecords.add(
          VacationDetailRequest.builder()
              .idCard(record.getIdCard())
              .name(record.getName())
              .department(record.getDepartment())
              .startDate(item.getStartDate())
              .endDate(item.getEndDate())
              .vacationType(vacationType)
              .vacationDays(finalVacationDays)
              // 以下字段保持为空
              .startTime("")
              .endTime("")
              .annualLeaveYear("")
              .childName("")
              .remark("")
              .build());
    }

    return detailRecords;
  }

  private List<VacationDetailItem> parseVacationDetails(String correctedRemark) {
    if (correctedRemark == null || correctedRemark.trim().isEmpty()) {
      return new ArrayList<>();
    }

    List<VacationDetailItem> items = new ArrayList<>();

    try {
      // 按分号分割多条记录
      String[] records = correctedRemark.split("[；;]");

      for (String record : records) {
        String trimmedRecord = record.trim();
        if (trimmedRecord.isEmpty()) {
          continue;
        }

        VacationDetailItem item = parseVacationRecord(trimmedRecord);
        if (item != null) {
          items.add(item);
        }
      }

      log.info("备注拆解 - 输入: {}, 拆解结果: {}条记录", correctedRemark, items.size());

    } catch (Exception e) {
      log.error("备注拆解失败: {}", correctedRemark, e);
    }

    return items;
  }

  private VacationDetailItem parseVacationRecord(String record) {
    try {
      // 使用正则表达式解析: 日期 休假类型 数量 单位
      // 日期可能是单日期(2025/9/2)或日期范围(2025/9/16-2025/9/30)
      String[] parts = record.split("\\s+");

      if (parts.length < 4) {
        log.warn("记录格式不正确，跳过: {}", record);
        return null;
      }

      String dateStr = parts[0];
      String vacationType = parts[1];
      String quantity = parts[2];
      String unit = parts[3];

      // 解析日期
      String startDate;
      String endDate;

      if (dateStr.contains("-")) {
        // 日期范围: 2025/9/16-2025/9/30
        String[] dates = dateStr.split("-");
        startDate = formatDate(dates[0].trim());
        endDate = formatDate(dates[1].trim());
      } else {
        // 单日期: 2025/9/2
        startDate = formatDate(dateStr);
        endDate = startDate;
      }

      String vacationDays = formatVacationDays(quantity, unit);

      return VacationDetailItem.builder()
          .startDate(startDate)
          .endDate(endDate)
          .vacationType(vacationType)
          .vacationDays(vacationDays)
          .build();

    } catch (Exception e) {
      log.error("解析休假记录失败: {}", record, e);
      return null;
    }
  }

  private String formatVacationDays(String quantity, String unit) {
    if (quantity == null || quantity.isEmpty()) {
      return "";
    }

    String normalizedQuantity =
        quantity.replace("，", ",").replace(',', '.').replaceAll("[^0-9.]+", "");
    if (normalizedQuantity.isEmpty()) {
      return quantity;
    }

    try {
      BigDecimal value = new BigDecimal(normalizedQuantity);
      if (unit != null && unit.contains("小")) {
        value = value.divide(HOURS_PER_DAY, 4, RoundingMode.HALF_UP);
      }
      return value.stripTrailingZeros().toPlainString();
    } catch (NumberFormatException ex) {
      log.warn("无法解析休假天数，保持原值: {}{}", quantity, unit, ex);
      return quantity;
    }
  }

  private String formatDate(String dateStr) {
    try {
      // 处理 2025/9/2 格式
      String[] parts = dateStr.split("/");
      if (parts.length == 3) {
        String year = parts[0];
        String month = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
        String day = parts[2].length() == 1 ? "0" + parts[2] : parts[2];
        return year + "-" + month + "-" + day;
      }
      return dateStr;
    } catch (Exception e) {
      log.error("日期格式化失败: {}", dateStr, e);
      return dateStr;
    }
  }

  private boolean shouldUseFixedOneDay(String vacationType) {
    if (vacationType == null || vacationType.isBlank()) {
      return false;
    }

    String normalizedType = vacationType.trim();
    if (FIXED_ONE_DAY_VACATION_TYPES.contains(normalizedType)) {
      return true;
    }

    String[] parts = normalizedType.split("[、,，/;；\\s]+");
    if (parts.length > 1) {
      for (String part : parts) {
        String candidate = part.trim();
        if (!candidate.isEmpty() && FIXED_ONE_DAY_VACATION_TYPES.contains(candidate)) {
          return true;
        }
      }
    }
    return false;
  }
}
