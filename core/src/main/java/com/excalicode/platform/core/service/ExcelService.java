package com.excalicode.platform.core.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
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
import com.excalicode.platform.common.exception.BusinessException;
import com.excalicode.platform.core.dto.CosmicProcessDto;
import lombok.extern.slf4j.Slf4j;

/**
 * Excel 报告生成服务
 */
@Slf4j
@Service
public class ExcelService {

    private static final String[] EXCEL_HEADERS =
            {"客户需求", "功能用户", "功能用户需求", "触发事件", "功能过程", "子过程描述", "数据移动类型", "数据组", "数据属性", "CFP"};
    // 列宽单位：1个字符宽度 ≈ 256
    private static final int[] COLUMN_WIDTHS = {25 * 256, 25 * 256, 25 * 256, // 客户需求、功能用户、功能用户需求
            25 * 256, 25 * 256, // 触发事件、功能过程
            40 * 256, // 子过程描述
            15 * 256, // 数据移动类型
            30 * 256, // 数据组
            60 * 256, // 数据属性
            15 * 256 // CFP
    };
    private static final short HEADER_FONT_SIZE = 12;
    private static final short ROW_HEIGHT = 22 * 20; // 行高单位：1/20个点

    public byte[] generateExcelReport(List<CosmicProcessDto> processes) {
        if (processes == null || processes.isEmpty()) {
            throw new BusinessException("COSMIC过程列表不能为空");
        }

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 创建时序图sheet（空白）
            workbook.createSheet("时序图");

            // 创建功能点拆分表sheet
            Sheet sheet = workbook.createSheet("功能点拆分表");

            // 设置列宽
            for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
                sheet.setColumnWidth(i, COLUMN_WIDTHS[i]);
            }

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle centerDataStyle = createCenterDataStyle(workbook);

            // 创建表头
            createExcelHeader(sheet, headerStyle);

            // 填充数据
            fillData(sheet, processes, dataStyle, centerDataStyle);

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
        for (int i = 0; i < EXCEL_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(EXCEL_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void fillData(Sheet sheet, List<CosmicProcessDto> processes, CellStyle dataStyle,
            CellStyle centerDataStyle) {
        int rowNum = 1;
        int totalRows = processes.size();
        String currentTriggerEvent = "";
        String currentFunctionalProcess = "";
        int triggerEventStartRow = 1;
        int functionalProcessStartRow = 1;

        for (int i = 0; i < processes.size(); i++) {
            CosmicProcessDto process = processes.get(i);
            Row row = sheet.createRow(rowNum);
            row.setHeight(ROW_HEIGHT);

            // 前三列空白
            row.createCell(0).setCellValue("");
            row.createCell(1).setCellValue("");
            row.createCell(2).setCellValue("");

            // 原有数据列（索引3-8）
            row.createCell(3).setCellValue(process.getTriggerEvent());
            row.createCell(4).setCellValue(process.getFunctionalProcess());
            row.createCell(5).setCellValue(process.getSubProcessDesc());
            row.createCell(6).setCellValue(process.getDataMovementType());
            row.createCell(7).setCellValue(process.getDataGroup());
            row.createCell(8).setCellValue(process.getDataAttributes());

            // CFP列默认值1
            row.createCell(9).setCellValue(1);

            // 应用样式
            for (int j = 0; j < 10; j++) {
                // 数据移动类型列（索引6）和CFP列（索引9）使用居中样式
                if (j == 6 || j == 9) {
                    row.getCell(j).setCellStyle(centerDataStyle);
                } else {
                    row.getCell(j).setCellStyle(dataStyle);
                }
            }

            if (!process.getTriggerEvent().equals(currentTriggerEvent)) {
                if (rowNum > 1 && triggerEventStartRow < rowNum - 1) {
                    sheet.addMergedRegion(
                            new CellRangeAddress(triggerEventStartRow, rowNum - 1, 3, 3));
                }
                currentTriggerEvent = process.getTriggerEvent();
                triggerEventStartRow = rowNum;
            }

            if (!process.getFunctionalProcess().equals(currentFunctionalProcess)) {
                if (rowNum > 1 && functionalProcessStartRow < rowNum - 1) {
                    sheet.addMergedRegion(
                            new CellRangeAddress(functionalProcessStartRow, rowNum - 1, 4, 4));
                }
                currentFunctionalProcess = process.getFunctionalProcess();
                functionalProcessStartRow = rowNum;
            }

            if (i == processes.size() - 1) {
                if (triggerEventStartRow < rowNum) {
                    sheet.addMergedRegion(new CellRangeAddress(triggerEventStartRow, rowNum, 3, 3));
                }
                if (functionalProcessStartRow < rowNum) {
                    sheet.addMergedRegion(
                            new CellRangeAddress(functionalProcessStartRow, rowNum, 4, 4));
                }
            }

            rowNum++;
        }

        // 合并前三列（客户需求、功能用户、功能用户需求）从第一行数据到最后一行数据
        if (totalRows > 0) {
            sheet.addMergedRegion(new CellRangeAddress(1, totalRows, 0, 0)); // 客户需求
            sheet.addMergedRegion(new CellRangeAddress(1, totalRows, 1, 1)); // 功能用户
            sheet.addMergedRegion(new CellRangeAddress(1, totalRows, 2, 2)); // 功能用户需求
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.RED.getIndex());
        font.setFontHeightInPoints(HEADER_FONT_SIZE);
        style.setFont(font);

        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createCenterDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}
