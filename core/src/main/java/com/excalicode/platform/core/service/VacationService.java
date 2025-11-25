package com.excalicode.platform.core.service;

import com.excalicode.platform.common.enums.AiFunctionType;
import com.excalicode.platform.common.exception.BusinessException;
import com.excalicode.platform.core.ai.AiFunctionExecutor;
import com.excalicode.platform.core.api.vacation.VacationDetailItem;
import com.excalicode.platform.core.api.vacation.VacationDetailRequest;
import com.excalicode.platform.core.api.vacation.VacationRecordRequest;
import com.excalicode.platform.core.api.vacation.VacationSplitResponse;
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
import java.util.concurrent.Semaphore;
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

  /** 需要识别的列名映射 */
  private static final Map<String, String> COLUMN_MAPPING =
      new HashMap<>() {
        {
          put("身份证号码", "idCard");
          put("姓名", "name");
          put("一级部门", "department");
          put("备注", "remark");
        }
      };

  private static final int MAX_CONCURRENT_CORRECTIONS = 5;
  private static final Set<String> FIXED_ONE_DAY_VACATION_TYPES = Set.of("迟到", "迟到早退", "未刷卡");
  private static final BigDecimal HOURS_PER_DAY = new BigDecimal("8");
  private final AiFunctionExecutor aiFunctionExecutor;
  private final ExecutorService applicationExecutorService;
  private final Semaphore correctionConcurrencyLimiter =
      new Semaphore(MAX_CONCURRENT_CORRECTIONS, true);

  /**
   * 解析员工休假记录Excel文件
   *
   * @param file Excel文件
   * @return 休假记录拆分结果
   */
  public VacationSplitResponse parseVacationExcel(MultipartFile file) {
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
      List<VacationRecordRequest> allRecords = new ArrayList<>();
      List<VacationRecordRequest> validRecords = new ArrayList<>();

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }

        VacationRecordRequest record = parseRow(row, columnIndexMap);
        allRecords.add(record);

        // 只返回备注不为空的记录
        if (record.getRemark() != null && !record.getRemark().trim().isEmpty()) {
          validRecords.add(record);
        }
      }

      log.info("解析Excel完成,总记录数:{},有效记录数:{}", allRecords.size(), validRecords.size());

      return VacationSplitResponse.builder()
          .records(validRecords)
          .totalCount(allRecords.size())
          .validCount(validRecords.size())
          .build();

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

  /** 解析表头,获取目标列的索引 */
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

  /** 验证必需的列是否存在 */
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

  /** 解析数据行 */
  private VacationRecordRequest parseRow(Row row, Map<String, Integer> columnIndexMap) {
    return VacationRecordRequest.builder()
        .idCard(getCellValue(row, columnIndexMap.get("idCard")))
        .name(getCellValue(row, columnIndexMap.get("name")))
        .department(getCellValue(row, columnIndexMap.get("department")))
        .remark(getCellValue(row, columnIndexMap.get("remark")))
        .build();
  }

  /** 获取单元格的值 */
  private String getCellValue(Row row, Integer columnIndex) {
    if (columnIndex == null) {
      return "";
    }

    Cell cell = row.getCell(columnIndex);
    return getCellValueAsString(cell);
  }

  /** 将单元格值转换为字符串 */
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

  /**
   * 使用AI修正备注
   *
   * @param remark 原始备注
   * @return 修正后的备注
   */
  public String correctRemark(String remark) {
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

  /**
   * 批量修正备注
   *
   * @param records 休假记录列表
   * @return 修正后的休假记录列表
   */
  public List<VacationRecordRequest> correctRemarks(List<VacationRecordRequest> records) {
    if (records == null || records.isEmpty()) {
      return records;
    }

    log.info("开始批量修正备注，共{}条记录", records.size());

    List<VacationRecordRequest> candidates =
        records.stream()
            .filter(record -> record.getRemark() != null && !record.getRemark().trim().isEmpty())
            .toList();

    if (candidates.isEmpty()) {
      log.info("批量修正备注完成 - 无需处理有效备注");
      return records;
    }

    List<CompletableFuture<Void>> futures = new ArrayList<>(candidates.size());
    for (VacationRecordRequest record : candidates) {
      futures.add(
          CompletableFuture.runAsync(
              () -> {
                boolean acquired = false;
                try {
                  correctionConcurrencyLimiter.acquire();
                  acquired = true;
                  String correctedRemark = correctRemark(record.getRemark());
                  record.setCorrectedRemark(
                      correctedRemark != null ? correctedRemark : record.getRemark());
                } catch (InterruptedException ie) {
                  Thread.currentThread().interrupt();
                  log.warn("备注修正任务被中断，保留原始备注: {}", record.getRemark(), ie);
                  record.setCorrectedRemark(record.getRemark());
                } catch (Exception ex) {
                  log.error("备注修正任务异常，保留原始备注: {}", record.getRemark(), ex);
                  record.setCorrectedRemark(record.getRemark());
                } finally {
                  if (acquired) {
                    correctionConcurrencyLimiter.release();
                  }
                }
              },
              applicationExecutorService));
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    log.info("批量修正备注完成");
    return records;
  }

  private String invokeAiCorrection(String remark) {
    return aiFunctionExecutor.executeText(AiFunctionType.QINSHI_DATA_PROCESSING, remark);
  }

  /**
   * 拆解单条修正后的备注为多条休假记录（基于规则解析） 格式: 日期 休假类型 数量 单位；日期 休假类型 数量 单位 例如: 2025/9/2 调休 1 天；2025/9/9 调休 3.5
   * 小时；2025/9/16-2025/9/30 陪产假 15 天
   *
   * @param correctedRemark 修正后的备注
   * @return 休假记录项列表
   */
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

  /**
   * 解析单条休假记录 格式: 日期 休假类型 数量 单位 例如: 2025/9/2 调休 1 天 或 2025/9/16-2025/9/30 陪产假 15 天
   *
   * @param record 单条记录
   * @return 休假记录项
   */
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

  /** 将数量和单位转换成以天为单位的数值字符串，小时按照 8 小时折算 1 天。 */
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

  /** 格式化日期为标准格式 YYYY-MM-DD。 */
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

  /**
   * 生成休假数据表
   *
   * @param records 带有修正备注的记录列表
   * @return 休假详细记录列表
   */
  public List<VacationDetailRequest> generateVacationDetailTable(
      List<VacationRecordRequest> records) {
    if (records == null || records.isEmpty()) {
      return new ArrayList<>();
    }

    log.info("开始生成休假数据表，共{}条记录", records.size());

    List<VacationDetailRequest> detailRecords = new ArrayList<>();

    for (VacationRecordRequest record : records) {
      // 如果没有修正后的备注，跳过
      if (record.getCorrectedRemark() == null || record.getCorrectedRemark().trim().isEmpty()) {
        log.warn("记录缺少修正备注，跳过：{} - {}", record.getName(), record.getIdCard());
        continue;
      }

      // 拆解修正后的备注
      List<VacationDetailItem> items = parseVacationDetails(record.getCorrectedRemark());

      // 为每个拆解项创建一条详细记录
      for (VacationDetailItem item : items) {
        String vacationType = item.getVacationType();
        String finalVacationDays =
            shouldUseFixedOneDay(vacationType) ? "1" : item.getVacationDays();

        VacationDetailRequest detailRecord =
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
                .build();

        detailRecords.add(detailRecord);
      }
    }

    log.info("休假数据表生成完成，共生成{}条详细记录", detailRecords.size());
    return detailRecords;
  }
}
