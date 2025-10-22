package com.excalicode.platform.core.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.springframework.stereotype.Service;
import com.excalicode.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PRD 产品需求文档服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrdService {

    // 正则表达式匹配模式
    private static final Pattern FIRST_LEVEL_LIST = Pattern.compile("^\\d+、.*");
    private static final Pattern SECOND_LEVEL_LIST = Pattern.compile("^\\(\\d+\\).*");
    private static final Pattern THIRD_LEVEL_LIST = Pattern.compile("^[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳].*");

    // 章节标题
    private static final String[] SECTION_HEADERS = {"需求背景", "需求概述", "需求详情"};

    // 缩进值（单位：twips，1英寸=1440twips）
    private static final int FIRST_LEVEL_INDENT = 0;
    private static final int SECOND_LEVEL_INDENT = 360; // 0.25英寸
    private static final int THIRD_LEVEL_INDENT = 720; // 0.5英寸

    // 固定行距（单位：twips，22磅 = 440 twips）
    private static final int FIXED_LINE_SPACING = 440; // 22磅

    // 段落间距（单位：twips）
    private static final int PARAGRAPH_SPACING = 50; // 列表项和普通文本的段后间距

    public byte[] generateWordDocument(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("文档内容不能为空");
        }

        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            String[] lines = content.split("\n");
            boolean isFirstNonEmptyLine = true;

            for (String line : lines) {
                // 跳过空行
                if (line.trim().isEmpty()) {
                    // 仍然创建空段落以保持格式
                    document.createParagraph();
                    continue;
                }

                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                paragraph.setAlignment(ParagraphAlignment.LEFT);

                // 设置固定行距为 22 磅（440 twips）
                if (paragraph.getCTP().getPPr() == null) {
                    paragraph.getCTP().addNewPPr();
                }
                if (paragraph.getCTP().getPPr().getSpacing() == null) {
                    paragraph.getCTP().getPPr().addNewSpacing();
                }
                paragraph.getCTP().getPPr().getSpacing()
                        .setLine(BigInteger.valueOf(FIXED_LINE_SPACING));
                paragraph.getCTP().getPPr().getSpacing().setLineRule(STLineSpacingRule.EXACT);

                String trimmedLine = line.trim();

                // 第一行非空行：需求名称（Heading 2）
                if (isFirstNonEmptyLine) {
                    run.setText(trimmedLine);
                    run.setFontSize(16);
                    run.setBold(true);
                    run.setFontFamily("Microsoft YaHei");
                    paragraph.setSpacingAfter(200); // 标题后间距稍大
                    isFirstNonEmptyLine = false;
                }
                // 章节标题：需求背景/需求概述/需求详情（Heading 3）
                else if (isSectionHeader(trimmedLine)) {
                    run.setText(trimmedLine);
                    run.setFontSize(14);
                    run.setBold(true);
                    run.setFontFamily("Microsoft YaHei");
                    paragraph.setSpacingAfter(150); // 章节标题后间距适中
                }
                // 一级编号列表：1、2、3、
                else if (FIRST_LEVEL_LIST.matcher(trimmedLine).matches()) {
                    run.setText(trimmedLine);
                    run.setFontSize(11);
                    run.setFontFamily("Microsoft YaHei");
                    paragraph.setIndentationLeft(FIRST_LEVEL_INDENT);
                    paragraph.setSpacingAfter(PARAGRAPH_SPACING);
                }
                // 二级编号列表：(1) (2) (3)
                else if (SECOND_LEVEL_LIST.matcher(trimmedLine).matches()) {
                    run.setText(trimmedLine);
                    run.setFontSize(11);
                    run.setFontFamily("Microsoft YaHei");
                    paragraph.setIndentationLeft(SECOND_LEVEL_INDENT);
                    paragraph.setSpacingAfter(PARAGRAPH_SPACING);
                }
                // 三级编号列表：① ② ③
                else if (THIRD_LEVEL_LIST.matcher(trimmedLine).matches()) {
                    run.setText(trimmedLine);
                    run.setFontSize(11);
                    run.setFontFamily("Microsoft YaHei");
                    paragraph.setIndentationLeft(THIRD_LEVEL_INDENT);
                    paragraph.setSpacingAfter(PARAGRAPH_SPACING);
                }
                // 普通文本
                else {
                    run.setText(trimmedLine);
                    run.setFontSize(11);
                    run.setFontFamily("Microsoft YaHei");
                    paragraph.setSpacingAfter(PARAGRAPH_SPACING);
                }
            }

            document.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("生成Word文档字节流时发生IO错误", e);
            throw new BusinessException("生成Word文档时发生错误: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("生成Word文档字节流时发生未知错误", e);
            throw new BusinessException("生成Word文档时发生未知错误: " + e.getMessage(), e);
        }
    }

    /**
     * 判断是否为章节标题
     */
    private boolean isSectionHeader(String line) {
        for (String header : SECTION_HEADERS) {
            if (header.equals(line)) {
                return true;
            }
        }
        return false;
    }

}
