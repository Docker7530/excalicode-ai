package com.excalicode.platform.core.service;

import com.excalicode.platform.core.exception.BusinessException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.springframework.stereotype.Service;

/** PRD 产品需求文档服务 */
@Slf4j
@Service
public class CosmicPrdService {

  // 固定行距（单位：twips，22磅 = 440 twips）
  private static final int FIXED_LINE_SPACING = 440;

  // 样式配置:用数据结构消除重复代码
  private static final StyleConfig TITLE_STYLE = new StyleConfig(16, true, 0, 200);
  private static final StyleConfig SECTION_STYLE = new StyleConfig(14, true, 0, 150);
  private static final StyleConfig FIRST_LEVEL_STYLE = new StyleConfig(11, false, 0, 50);
  private static final StyleConfig SECOND_LEVEL_STYLE = new StyleConfig(11, false, 360, 50);
  private static final StyleConfig THIRD_LEVEL_STYLE = new StyleConfig(11, false, 720, 50);
  private static final StyleConfig NORMAL_STYLE = new StyleConfig(11, false, 0, 50);

  // 章节标题集合:用Set消除循环
  private static final Set<String> SECTION_HEADERS = Set.of("需求背景", "需求概述", "需求详情");

  // 样式匹配规则:用数据结构消除if-else
  private static final List<StyleRule> STYLE_RULES =
      Arrays.asList(
          new StyleRule(Pattern.compile("^\\d+、.*"), FIRST_LEVEL_STYLE),
          new StyleRule(Pattern.compile("^\\(\\d+\\).*"), SECOND_LEVEL_STYLE),
          new StyleRule(Pattern.compile("^[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳].*"), THIRD_LEVEL_STYLE));

  public byte[] generateWordDocument(String content) {
    if (content == null || content.trim().isEmpty()) {
      throw new BusinessException("文档内容不能为空");
    }

    try (XWPFDocument document = new XWPFDocument();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      String[] lines = content.split("\n");
      boolean isFirstNonEmptyLine = true;

      for (String line : lines) {
        if (line.trim().isEmpty()) {
          document.createParagraph();
          continue;
        }

        String trimmedLine = line.trim();
        StyleConfig style = determineStyle(trimmedLine, isFirstNonEmptyLine);

        createStyledParagraph(document, trimmedLine, style);

        if (isFirstNonEmptyLine) {
          isFirstNonEmptyLine = false;
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

  /** 确定文本应该使用的样式:用数据查找替代条件分支 */
  private StyleConfig determineStyle(String text, boolean isFirstLine) {
    if (isFirstLine) {
      return TITLE_STYLE;
    }
    if (SECTION_HEADERS.contains(text)) {
      return SECTION_STYLE;
    }
    // 遍历规则列表,找到第一个匹配的样式
    for (StyleRule rule : STYLE_RULES) {
      if (rule.matches(text)) {
        return rule.style;
      }
    }
    return NORMAL_STYLE;
  }

  /** 创建带样式的段落:提取重复的段落设置逻辑 */
  private void createStyledParagraph(XWPFDocument document, String text, StyleConfig style) {
    XWPFParagraph paragraph = document.createParagraph();
    paragraph.setAlignment(ParagraphAlignment.LEFT);
    setupLineSpacing(paragraph);

    XWPFRun run = paragraph.createRun();
    run.setText(text);
    run.setFontSize(style.fontSize);
    run.setBold(style.bold);
    run.setFontFamily("Microsoft YaHei");

    paragraph.setIndentationLeft(style.indent);
    paragraph.setSpacingAfter(style.spacingAfter);
  }

  /** 设置段落固定行距:提取笨拙的POI API调用 */
  private void setupLineSpacing(XWPFParagraph paragraph) {
    if (paragraph.getCTP().getPPr() == null) {
      paragraph.getCTP().addNewPPr();
    }
    if (paragraph.getCTP().getPPr().getSpacing() == null) {
      paragraph.getCTP().getPPr().addNewSpacing();
    }
    paragraph.getCTP().getPPr().getSpacing().setLine(BigInteger.valueOf(FIXED_LINE_SPACING));
    paragraph.getCTP().getPPr().getSpacing().setLineRule(STLineSpacingRule.EXACT);
  }

  /** 样式配置:所有格式参数的封装 */
  private record StyleConfig(int fontSize, boolean bold, int indent, int spacingAfter) {}

  /** 样式规则:模式匹配和样式的绑定 */
  private record StyleRule(Pattern pattern, StyleConfig style) {
    boolean matches(String text) {
      return pattern.matcher(text).matches();
    }
  }
}
