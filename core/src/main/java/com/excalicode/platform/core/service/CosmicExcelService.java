package com.excalicode.platform.core.service;

import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.cosmic.CosmicProcess;
import com.excalicode.platform.core.model.cosmic.CosmicProcessStep;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/** Excel 报告生成服务 */
@Slf4j
@Service
public class CosmicExcelService {

  // 行高单位：1/20个点
  private static final short ROW_HEIGHT = 22 * 20;
  private static final short HEADER_FONT_SIZE = 12;

  public byte[] generateExcelReport(List<CosmicProcess> processes) {
    if (processes == null || processes.isEmpty()) {
      throw new BusinessException("COSMIC过程列表不能为空");
    }

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      // 创建时序图sheet（空白）
      workbook.createSheet("时序图");

      // 创建功能点拆分表sheet
      Sheet sheet = workbook.createSheet("功能点拆分表");

      // 设置列宽:从枚举获取配置
      for (ColumnDef col : ColumnDef.all()) {
        sheet.setColumnWidth(col.ordinal(), col.width);
      }

      // 创建样式
      CellStyle headerStyle = createHeaderStyle(workbook);
      CellStyle leftStyle = createDataStyle(workbook, HorizontalAlignment.LEFT);
      CellStyle centerStyle = createDataStyle(workbook, HorizontalAlignment.CENTER);

      // 创建表头
      createExcelHeader(sheet, headerStyle);

      // 填充数据
      fillData(sheet, processes, leftStyle, centerStyle);

      workbook.write(outputStream);
      return outputStream.toByteArray();

    } catch (IOException e) {
      log.error("生成Excel字节流时发生IO错误", e);
      throw new BusinessException("生成Excel字节流时发生错误: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("生成Excel字节流时发生未知错误", e);
      throw new BusinessException("生成Excel字节流时发生未知错误: " + e.getMessage(), e);
    }
  }

  private void createExcelHeader(Sheet sheet, CellStyle headerStyle) {
    Row headerRow = sheet.createRow(0);
    headerRow.setHeight(ROW_HEIGHT);
    for (ColumnDef col : ColumnDef.all()) {
      Cell cell = headerRow.createCell(col.ordinal());
      cell.setCellValue(col.header);
      cell.setCellStyle(headerStyle);
    }
  }

  private void fillData(
      Sheet sheet, List<CosmicProcess> processes, CellStyle leftStyle, CellStyle centerStyle) {
    int rowNum = 1;
    MergeTracker triggerTracker = new MergeTracker(ColumnDef.TRIGGER_EVENT.ordinal());
    MergeTracker processTracker = new MergeTracker(ColumnDef.FUNCTIONAL_PROCESS.ordinal());

    for (CosmicProcess process : processes) {
      String triggerEvent = process.getTriggerEvent();
      String functionalProcess = process.getFunctionalProcess();

      List<CosmicProcessStep> steps = process.getProcessSteps();
      for (int i = 0; i < steps.size(); i++) {
        CosmicProcessStep step = steps.get(i);
        Row row = sheet.createRow(rowNum);
        row.setHeight(ROW_HEIGHT);

        // 填充单元格:用枚举消除硬编码索引
        setCellValue(row, ColumnDef.CUSTOMER_REQ, "", leftStyle);
        setCellValue(row, ColumnDef.FUNCTIONAL_USER, "", leftStyle);
        setCellValue(row, ColumnDef.USER_REQ, "", leftStyle);
        setCellValue(row, ColumnDef.TRIGGER_EVENT, triggerEvent, leftStyle);
        setCellValue(row, ColumnDef.FUNCTIONAL_PROCESS, functionalProcess, leftStyle);
        setCellValue(row, ColumnDef.SUB_PROCESS, step.getSubProcessDesc(), leftStyle);
        setCellValue(row, ColumnDef.DATA_MOVEMENT, step.getDataMovementType(), centerStyle);
        setCellValue(row, ColumnDef.DATA_GROUP, step.getDataGroup(), leftStyle);
        setCellValue(row, ColumnDef.DATA_ATTRS, step.getDataAttributes(), leftStyle);
        setCellValue(row, ColumnDef.CFP, "1", centerStyle);

        // 跟踪合并区域
        boolean isFirstStep = (i == 0);
        if (isFirstStep) {
          triggerTracker.tryFinishAndStart(sheet, triggerEvent, rowNum);
          processTracker.tryFinishAndStart(sheet, functionalProcess, rowNum);
        }

        rowNum++;
      }
    }

    // 完成最后的合并
    triggerTracker.finish(sheet, rowNum - 1);
    processTracker.finish(sheet, rowNum - 1);

    // 合并前三列:客户需求、功能用户、功能用户需求
    int totalRows = rowNum - 1;
    if (totalRows > 1) {
      sheet.addMergedRegion(
          new CellRangeAddress(
              1, totalRows, ColumnDef.CUSTOMER_REQ.ordinal(), ColumnDef.CUSTOMER_REQ.ordinal()));
      sheet.addMergedRegion(
          new CellRangeAddress(
              1,
              totalRows,
              ColumnDef.FUNCTIONAL_USER.ordinal(),
              ColumnDef.FUNCTIONAL_USER.ordinal()));
      sheet.addMergedRegion(
          new CellRangeAddress(
              1, totalRows, ColumnDef.USER_REQ.ordinal(), ColumnDef.USER_REQ.ordinal()));
    }
  }

  /** 设置单元格值和样式:消除重复的createCell和setStyle调用 */
  private void setCellValue(Row row, ColumnDef col, String value, CellStyle style) {
    Cell cell = row.createCell(col.ordinal());
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  /** 创建表头样式 */
  private CellStyle createHeaderStyle(Workbook workbook) {
    CellStyle style = createBaseBorderedStyle(workbook);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);

    Font font = workbook.createFont();
    font.setBold(true);
    font.setColor(IndexedColors.RED.getIndex());
    font.setFontHeightInPoints(HEADER_FONT_SIZE);
    style.setFont(font);

    return style;
  }

  /** 创建数据样式:消除createDataStyle和createCenterDataStyle的重复 */
  private CellStyle createDataStyle(Workbook workbook, HorizontalAlignment alignment) {
    CellStyle style = createBaseBorderedStyle(workbook);
    style.setWrapText(true);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    if (alignment != null) {
      style.setAlignment(alignment);
    }
    return style;
  }

  /** 创建基础边框样式:提取重复的边框设置 */
  private CellStyle createBaseBorderedStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }

  // 列定义:用数据结构消除硬编码的列索引
  private enum ColumnDef {
    CUSTOMER_REQ("客户需求", 25),
    FUNCTIONAL_USER("功能用户", 25),
    USER_REQ("功能用户需求", 25),
    TRIGGER_EVENT("触发事件", 25),
    FUNCTIONAL_PROCESS("功能过程", 25),
    SUB_PROCESS("子过程描述", 40),
    DATA_MOVEMENT("数据移动类型", 15),
    DATA_GROUP("数据组", 30),
    DATA_ATTRS("数据属性", 60),
    CFP("CFP", 15);

    final String header;
    final int width; // 字符数

    ColumnDef(String header, int widthInChars) {
      this.header = header;
      this.width = widthInChars * 256; // 转换为POI的宽度单位
    }

    static ColumnDef[] all() {
      return values();
    }
  }

  /** 合并区域跟踪器:消除重复的合并逻辑 */
  private static class MergeTracker {
    private final int columnIndex;
    private String currentValue = null;
    private int startRow = -1;

    MergeTracker(int columnIndex) {
      this.columnIndex = columnIndex;
    }

    /** 尝试完成当前合并并开始新的合并 */
    void tryFinishAndStart(Sheet sheet, String newValue, int currentRow) {
      if (currentValue != null && !currentValue.equals(newValue)) {
        finishMerge(sheet, currentRow - 1);
      }
      if (!newValue.equals(currentValue)) {
        currentValue = newValue;
        startRow = currentRow;
      }
    }

    /** 完成最后的合并 */
    void finish(Sheet sheet, int lastRow) {
      if (currentValue != null) {
        finishMerge(sheet, lastRow);
      }
    }

    private void finishMerge(Sheet sheet, int endRow) {
      if (startRow >= 0 && startRow < endRow) {
        sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, columnIndex, columnIndex));
      }
    }
  }
}
