package com.excalicode.platform.core.service;

import com.excalicode.platform.core.ai.AiFunctionExecutor;
import com.excalicode.platform.core.api.cosmic.AnalysisResponse;
import com.excalicode.platform.core.api.cosmic.CosmicAnalysisRequest;
import com.excalicode.platform.core.api.cosmic.CosmicProcessesResponse;
import com.excalicode.platform.core.api.cosmic.DocumentExportRequest;
import com.excalicode.platform.core.api.cosmic.DocumentPreviewRequest;
import com.excalicode.platform.core.api.cosmic.FunctionalProcess;
import com.excalicode.platform.core.api.cosmic.FunctionalProcessesResponse;
import com.excalicode.platform.core.api.cosmic.ProcessBreakdownRequest;
import com.excalicode.platform.core.api.cosmic.ProcessBreakdownResponse;
import com.excalicode.platform.core.api.cosmic.ProcessTableExportRequest;
import com.excalicode.platform.core.api.cosmic.RequirementEnhanceRequest;
import com.excalicode.platform.core.api.cosmic.SequenceDiagramRequest;
import com.excalicode.platform.core.enums.AiFunctionType;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.cosmic.CosmicProcess;
import com.excalicode.platform.core.model.cosmic.CosmicProcessStep;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/** COSMIC 业务编排服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CosmicService {

  private static final String COSMIC_IMPORT_SHEET = "功能点拆分表";
  private static final String COSMIC_PROCESS_HEADER = "功能过程";
  private static final int COLUMN_TRIGGER_EVENT = 3;
  private static final int COLUMN_FUNCTIONAL_PROCESS = 4;
  private static final int COLUMN_SUB_PROCESS = 5;
  private static final int COLUMN_DATA_MOVEMENT = 6;
  private static final int COLUMN_DATA_GROUP = 7;
  private static final int COLUMN_DATA_ATTRIBUTES = 8;
  private static final Set<String> SUPPORTED_DATA_MOVEMENT_TYPES = Set.of("E", "R", "W", "X");

  private final AiFunctionExecutor aiFunctionExecutor;
  private final CosmicExcelService cosmicExcelService;
  private final CosmicPrdService cosmicPrdService;
  private final RequirementKnowledgeService requirementKnowledgeService;
  private final ObjectMapper objectMapper;

  /**
   * 流式返回需求扩写结果
   *
   * @param request 需求扩写请求参数
   * @return SSE 文本片段流，组成扩写后的需求描述
   */
  public Flux<String> streamEnhancedRequirement(RequirementEnhanceRequest request) {
    String originalRequirement = request == null ? null : request.getOriginalRequirement();
    if (!StringUtils.hasText(originalRequirement)) {
      return Flux.error(new BusinessException("原始需求描述不能为空"));
    }
    String normalized = originalRequirement.trim();
    Integer expectedProcessCount = request.getExpectedProcessCount();
    String ragContext = requirementKnowledgeService.buildContext(normalized);
    String userPrompt = buildRequirementEnhancePrompt(normalized, ragContext, expectedProcessCount);

    return aiFunctionExecutor
        .streamText(AiFunctionType.COSMIC_PM, userPrompt)
        .onErrorMap(
            error -> {
              if (error instanceof BusinessException) {
                return error;
              }
              return new BusinessException("AI 流式扩写失败: " + error.getMessage(), error);
            })
        .switchIfEmpty(Flux.error(new BusinessException("AI 未返回任何可用的扩写内容（流式）")));
  }

  /**
   * 执行功能过程拆解，使用 AI 将需求描述拆解为功能过程列表
   *
   * @param request 需求请求参数
   * @return 拆解结果，包含功能过程列表
   * @throws BusinessException 业务异常
   */
  public ProcessBreakdownResponse breakdownProcess(ProcessBreakdownRequest request) {
    String userPromptText =
        buildFunctionalProcessUserPrompt(
            request.getRequirementDescription(), request.getExpectedProcessCount());
    FunctionalProcessesResponse result =
        aiFunctionExecutor.executeStructured(
            AiFunctionType.COSMIC_FUNCTIONAL, userPromptText, FunctionalProcessesResponse.class);

    if (result == null || CollectionUtils.isEmpty(result.getFunctionalProcesses())) {
      throw new BusinessException("AI 拆解未能生成有效的功能过程");
    }
    List<FunctionalProcess> processes = result.getFunctionalProcesses();
    if (CollectionUtils.isEmpty(processes)) {
      throw new BusinessException("AI拆解未返回有效结果");
    }
    return ProcessBreakdownResponse.builder().functionalProcesses(processes).build();
  }

  private String buildFunctionalProcessUserPrompt(
      String requirementDescription, Integer expectedProcessCount) {
    StringBuilder builder = new StringBuilder();

    builder
        .append("需求描述如下：")
        .append(System.lineSeparator())
        .append(requirementDescription == null ? "" : requirementDescription.trim());

    if (expectedProcessCount != null && expectedProcessCount > 0) {
      builder
          .append(System.lineSeparator())
          .append("请在不遗漏关键业务场景的前提下，尽量将上述需求拆解为约 ")
          .append(expectedProcessCount)
          .append(" 个功能过程。");
    }

    return builder.toString();
  }

  private String buildRequirementEnhancePrompt(
      String originalRequirement, String ragContext, Integer expectedProcessCount) {
    StringBuilder builder = new StringBuilder();
    if (StringUtils.hasText(ragContext)) {
      builder
          .append("# 项目背景资料")
          .append(System.lineSeparator())
          .append("（请在扩写时严格遵守以下背景中的术语定义、业务边界和技术约束）")
          .append(System.lineSeparator())
          .append("```text")
          .append(System.lineSeparator())
          .append(ragContext.trim())
          .append(System.lineSeparator())
          .append("```")
          .append(System.lineSeparator())
          .append(System.lineSeparator());
    }
    builder
        .append("# 原始需求描述")
        .append(System.lineSeparator())
        .append(originalRequirement)
        .append(System.lineSeparator());

    if (expectedProcessCount != null && expectedProcessCount > 0) {
      builder
          .append(System.lineSeparator())
          .append("# 期望的功能过程数量")
          .append(System.lineSeparator())
          .append("请在扩写内容中兼顾后续拆解，目标是约 ")
          .append(expectedProcessCount)
          .append(" 个功能过程。")
          .append(System.lineSeparator());
    }
    return builder.toString();
  }

  /**
   * 从COSMIC Excel表格导入功能流程。
   *
   * @param file 上传的Excel文件
   * @return 解析得到的功能流程列表
   * @throws BusinessException 业务异常
   */
  public ProcessBreakdownResponse importFunctionalProcesses(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("请上传Excel文件");
    }
    String filename = file.getOriginalFilename();
    if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
      throw new BusinessException("文件格式异常。");
    }

    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheet(COSMIC_IMPORT_SHEET);
      if (sheet == null) {
        throw new BusinessException("Sheet '" + COSMIC_IMPORT_SHEET + "' not found");
      }
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) {
        throw new BusinessException("Sheet '" + COSMIC_IMPORT_SHEET + "' missing header row");
      }
      int processColumnIndex = locateFunctionalProcessColumn(headerRow);
      if (processColumnIndex < 0) {
        throw new BusinessException("Unable to locate functional process column");
      }
      List<FunctionalProcess> processes = extractFunctionalProcesses(sheet, processColumnIndex);
      if (processes.isEmpty()) {
        throw new BusinessException("Excel file does not contain valid functional processes");
      }
      return ProcessBreakdownResponse.builder().functionalProcesses(processes).build();
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to import functional processes from Excel", e);
      throw new BusinessException("Failed to parse Excel file: " + e.getMessage(), e);
    }
  }

  private int locateFunctionalProcessColumn(Row headerRow) {
    for (Cell cell : headerRow) {
      String header = getCellValueAsString(cell).trim();
      if (COSMIC_PROCESS_HEADER.equals(header)) {
        return cell.getColumnIndex();
      }
    }
    return -1;
  }

  private String getCellValueAsString(Cell cell) {
    if (cell == null) {
      return "";
    }
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> String.valueOf(cell.getNumericCellValue());
      case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
      case FORMULA -> cell.getCellFormula();
      default -> "";
    };
  }

  private List<FunctionalProcess> extractFunctionalProcesses(Sheet sheet, int columnIndex) {
    List<FunctionalProcess> processes = new ArrayList<>();
    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      // 跳过合并单元格中的非首行单元格，避免重复导入
      if (shouldSkipMergedCell(sheet, rowIndex, columnIndex)) {
        continue;
      }

      Row row = sheet.getRow(rowIndex);
      if (row == null) {
        continue;
      }
      String value = getCellValueAsString(row.getCell(columnIndex)).trim();
      if (StringUtils.hasText(value)) {
        processes.add(FunctionalProcess.builder().description(value).build());
      }
    }
    return processes;
  }

  /**
   * 判断指定单元格是否应该被跳过（合并单元格的非首单元格）
   *
   * @param sheet 工作表
   * @param rowIndex 行索引
   * @param columnIndex 列索引
   * @return true表示应跳过，false表示需要读取
   */
  private boolean shouldSkipMergedCell(Sheet sheet, int rowIndex, int columnIndex) {
    for (CellRangeAddress mergedRegion : sheet.getMergedRegions()) {
      if (mergedRegion.isInRange(rowIndex, columnIndex)) {
        // 如果在合并区域内，只有第一行第一列的单元格不跳过
        return !(rowIndex == mergedRegion.getFirstRow()
            && columnIndex == mergedRegion.getFirstColumn());
      }
    }
    return false; // 不在任何合并区域内，不跳过
  }

  /**
   * 导入 COSMIC 子过程表
   *
   * @param file 上传的 Excel 文件
   * @return 子过程列表
   */
  public AnalysisResponse importCosmicProcesses(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("请上传Excel文件");
    }
    String filename = file.getOriginalFilename();
    if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
      throw new BusinessException("文件格式异常。");
    }

    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheet(COSMIC_IMPORT_SHEET);
      if (sheet == null) {
        throw new BusinessException("Sheet '" + COSMIC_IMPORT_SHEET + "' not found");
      }
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) {
        throw new BusinessException("Sheet '" + COSMIC_IMPORT_SHEET + "' missing header row");
      }
      validateCosmicImportHeader(headerRow);
      List<CosmicProcess> processes = extractCosmicProcesses(sheet);
      if (processes.isEmpty()) {
        throw new BusinessException("Excel file does not contain valid COSMIC processes");
      }
      List<CosmicProcess> sanitized = sanitizeCosmicProcesses(processes);
      return AnalysisResponse.builder().processes(sanitized).build();
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to import COSMIC processes from Excel", e);
      throw new BusinessException("Failed to parse Excel file: " + e.getMessage(), e);
    }
  }

  private void validateCosmicImportHeader(Row headerRow) {
    String[] expectedHeaders = {"触发事件", "功能过程", "子过程描述", "数据移动类型", "数据组", "数据属性"};
    int[] columns = {
      COLUMN_TRIGGER_EVENT,
      COLUMN_FUNCTIONAL_PROCESS,
      COLUMN_SUB_PROCESS,
      COLUMN_DATA_MOVEMENT,
      COLUMN_DATA_GROUP,
      COLUMN_DATA_ATTRIBUTES
    };

    for (int i = 0; i < columns.length; i++) {
      String header = getCellValueAsString(headerRow.getCell(columns[i])).trim();
      if (!expectedHeaders[i].equals(header)) {
        throw new BusinessException("Excel 表头与模板不一致，期待列名: " + expectedHeaders[i]);
      }
    }
  }

  private List<CosmicProcess> extractCosmicProcesses(Sheet sheet) {
    Map<String, CosmicProcess> grouped = new LinkedHashMap<>();

    String currentTriggerEvent = "";
    String currentFunctionalProcess = "";

    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) {
        continue;
      }

      String triggerEvent = getCellValueAsString(row.getCell(COLUMN_TRIGGER_EVENT)).trim();
      if (StringUtils.hasText(triggerEvent)) {
        currentTriggerEvent = triggerEvent;
      }

      String functionalProcess =
          getCellValueAsString(row.getCell(COLUMN_FUNCTIONAL_PROCESS)).trim();
      if (StringUtils.hasText(functionalProcess)) {
        currentFunctionalProcess = functionalProcess;
      }

      String subProcessDesc = getCellValueAsString(row.getCell(COLUMN_SUB_PROCESS)).trim();
      String dataMovement = getCellValueAsString(row.getCell(COLUMN_DATA_MOVEMENT)).trim();
      String dataGroup = getCellValueAsString(row.getCell(COLUMN_DATA_GROUP)).trim();
      String dataAttributes = getCellValueAsString(row.getCell(COLUMN_DATA_ATTRIBUTES)).trim();

      boolean rowEmpty =
          !StringUtils.hasText(triggerEvent)
              && !StringUtils.hasText(functionalProcess)
              && !StringUtils.hasText(subProcessDesc)
              && !StringUtils.hasText(dataMovement)
              && !StringUtils.hasText(dataGroup)
              && !StringUtils.hasText(dataAttributes);
      if (rowEmpty) {
        continue;
      }

      if (!StringUtils.hasText(currentTriggerEvent)) {
        throw new BusinessException(String.format("第 %d 行缺少触发事件", rowIndex + 1));
      }
      if (!StringUtils.hasText(currentFunctionalProcess)) {
        throw new BusinessException(String.format("第 %d 行缺少功能过程", rowIndex + 1));
      }
      if (!StringUtils.hasText(subProcessDesc)) {
        throw new BusinessException(String.format("第 %d 行子过程描述不能为空", rowIndex + 1));
      }
      if (!StringUtils.hasText(dataMovement)) {
        throw new BusinessException(String.format("第 %d 行数据移动类型不能为空", rowIndex + 1));
      }
      if (!StringUtils.hasText(dataGroup)) {
        throw new BusinessException(String.format("第 %d 行数据组不能为空", rowIndex + 1));
      }

      final String triggerEventKey = currentTriggerEvent;
      final String functionalProcessKey = currentFunctionalProcess;
      String key = triggerEventKey + "||" + functionalProcessKey;
      CosmicProcess process =
          grouped.computeIfAbsent(
              key,
              ignored ->
                  CosmicProcess.builder()
                      .triggerEvent(triggerEventKey)
                      .functionalProcess(functionalProcessKey)
                      .processSteps(new ArrayList<>())
                      .build());

      process
          .getProcessSteps()
          .add(
              CosmicProcessStep.builder()
                  .subProcessDesc(subProcessDesc)
                  .dataMovementType(dataMovement)
                  .dataGroup(dataGroup)
                  .dataAttributes(dataAttributes)
                  .build());
    }

    return new ArrayList<>(grouped.values());
  }

  /**
   * 执行 COSMIC 分析：使用一次性生成所有字段的方式,可能在功能过程较多时出现截断问题。
   *
   * @param request 需求请求参数
   * @return 分析结果，包含 COSMIC 过程数据
   * @throws BusinessException 业务异常
   */
  public AnalysisResponse analyzeRequirement(CosmicAnalysisRequest request) {
    String userPromptText = buildCosmicUserPrompt(request.getFunctionalProcesses());
    CosmicProcessesResponse result =
        aiFunctionExecutor.executeStructured(
            AiFunctionType.COSMIC_ANALYSIS, userPromptText, CosmicProcessesResponse.class);

    if (result == null || CollectionUtils.isEmpty(result.getProcesses())) {
      throw new BusinessException("AI 分析未能生成有效的 COSMIC 过程");
    }
    List<CosmicProcess> processes = sanitizeCosmicProcesses(result.getProcesses());
    return AnalysisResponse.builder().processes(processes).build();
  }

  private String buildCosmicUserPrompt(List<FunctionalProcess> functionalProcesses) {
    StringBuilder builder = new StringBuilder();

    if (!CollectionUtils.isEmpty(functionalProcesses)) {
      builder.append("已确认的功能过程请参考下表：").append(System.lineSeparator());
      for (int i = 0; i < functionalProcesses.size(); i++) {
        FunctionalProcess process = functionalProcesses.get(i);
        builder
            .append(i + 1)
            .append(". ")
            .append(process.getDescription() == null ? "" : process.getDescription().trim());
        builder.append(System.lineSeparator());
      }
    }

    return builder.toString();
  }

  private List<CosmicProcess> sanitizeCosmicProcesses(List<CosmicProcess> processes) {
    if (CollectionUtils.isEmpty(processes)) {
      throw new BusinessException("COSMIC过程列表不能为空");
    }

    List<CosmicProcess> sanitized = new ArrayList<>();
    for (int processIndex = 0; processIndex < processes.size(); processIndex++) {
      CosmicProcess process = processes.get(processIndex);

      String triggerEvent = trimToEmpty(process.getTriggerEvent());
      if (!StringUtils.hasText(triggerEvent)) {
        throw new BusinessException(String.format("第 %d 个功能过程缺少触发事件", processIndex + 1));
      }

      String functionalProcess = trimToEmpty(process.getFunctionalProcess());
      if (!StringUtils.hasText(functionalProcess)) {
        throw new BusinessException(String.format("第 %d 个功能过程缺少功能过程名称", processIndex + 1));
      }

      List<CosmicProcessStep> steps = process.getProcessSteps();
      if (CollectionUtils.isEmpty(steps)) {
        throw new BusinessException(String.format("功能过程 \"%s\" 缺少子过程步骤", functionalProcess));
      }

      List<CosmicProcessStep> sanitizedSteps = new ArrayList<>();
      for (int stepIndex = 0; stepIndex < steps.size(); stepIndex++) {
        CosmicProcessStep step = steps.get(stepIndex);
        sanitizedSteps.add(sanitizeStep(step, functionalProcess, stepIndex));
      }

      sanitized.add(
          CosmicProcess.builder()
              .triggerEvent(triggerEvent)
              .functionalProcess(functionalProcess)
              .processSteps(sanitizedSteps)
              .build());
    }

    return sanitized;
  }

  private CosmicProcessStep sanitizeStep(
      CosmicProcessStep step, String functionalProcess, int stepIndex) {
    // 必填:子过程描述
    String subProcessDesc = trimToEmpty(step.getSubProcessDesc());
    if (!StringUtils.hasText(subProcessDesc)) {
      throw new BusinessException(
          String.format("功能过程 \"%s\" 的第 %d 个子过程描述为空", functionalProcess, stepIndex + 1));
    }

    // 必填:数据移动类型
    String dataMovementType =
        normalizeMovementType(step.getDataMovementType(), functionalProcess, stepIndex);

    // 选填:数据组(有就要合法)
    String dataGroup = trimToEmpty(step.getDataGroup());

    // 选填:数据属性
    String dataAttributes = trimToEmpty(step.getDataAttributes());

    return CosmicProcessStep.builder()
        .subProcessDesc(subProcessDesc)
        .dataMovementType(dataMovementType)
        .dataGroup(dataGroup)
        .dataAttributes(dataAttributes)
        .build();
  }

  private String normalizeMovementType(String value, String functionalProcess, int stepIndex) {
    String normalized = trimToEmpty(value).toUpperCase();
    if (!SUPPORTED_DATA_MOVEMENT_TYPES.contains(normalized)) {
      throw new BusinessException(
          String.format(
              "功能过程 \"%s\" 的第 %d 个子过程数据移动类型无效: %s", functionalProcess, stepIndex + 1, value));
    }
    return normalized;
  }

  private String trimToEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  /**
   * 导出功能过程表格为 Excel 字节数组
   *
   * @param request 文档生成请求参数
   * @return Excel 文件字节数组
   */
  public byte[] exportProcessTableAsBytes(ProcessTableExportRequest request) {
    List<CosmicProcess> sanitized = sanitizeCosmicProcesses(request.getProcesses());
    return cosmicExcelService.generateExcelReport(sanitized);
  }

  /**
   * 生成文档预览内容
   *
   * @param request 文档生成请求参数
   * @return 文档预览内容
   * @throws BusinessException 业务异常
   */
  public String generateDocumentPreview(DocumentPreviewRequest request) {
    List<CosmicProcess> processes = request.getProcesses();
    if (CollectionUtils.isEmpty(processes)) {
      throw new BusinessException("COSMIC过程列表不能为空");
    }
    List<CosmicProcess> sanitized = sanitizeCosmicProcesses(processes);

    String processDescription =
        sanitized.stream()
            .flatMap(
                p ->
                    p.getProcessSteps().stream()
                        .map(
                            step ->
                                String.format(
                                    "功能过程：%s，子过程：%s",
                                    p.getFunctionalProcess(), step.getSubProcessDesc())))
            .collect(Collectors.joining("；"));

    String requirementName = request.getRequirementName();
    if (!StringUtils.hasText(requirementName)) {
      throw new BusinessException("需求名称不能为空");
    }
    String userInput =
        String.format("需求名称：%s\n功能过程或子过程描述：%s", requirementName.trim(), processDescription);
    return aiFunctionExecutor.executeText(AiFunctionType.COSMIC_PRD, userInput);
  }

  /**
   * 生成 Mermaid 时序图语法
   *
   * @param request 功能过程与子过程描述
   * @return Mermaid sequenceDiagram 文本
   */
  public String generateSequenceDiagram(SequenceDiagramRequest request) {
    List<CosmicProcess> processes = request.getProcesses();
    if (CollectionUtils.isEmpty(processes)) {
      throw new BusinessException("COSMIC过程列表不能为空");
    }
    List<CosmicProcess> sanitized = sanitizeCosmicProcesses(processes);
    String userPrompt = serializeSequenceDiagramPayload(sanitized);
    String diagram =
        aiFunctionExecutor.executeText(AiFunctionType.COSMIC_SEQUENCE_DIAGRAM, userPrompt);
    return normalizeMermaidDiagram(diagram);
  }

  private String normalizeMermaidDiagram(String rawDiagram) {
    if (!StringUtils.hasText(rawDiagram)) {
      throw new BusinessException("AI 未返回时序图内容");
    }
    String trimmed = rawDiagram.trim();
    if (!trimmed.toLowerCase().contains("sequencediagram")) {
      throw new BusinessException("AI 输出不包含 Mermaid sequenceDiagram 片段");
    }
    return trimmed;
  }

  private String serializeSequenceDiagramPayload(List<CosmicProcess> processes) {
    try {
      return objectMapper.writeValueAsString(processes);
    } catch (JsonProcessingException ex) {
      log.error("序列化 COSMIC 子过程失败", ex);
      throw new BusinessException("COSMIC 子过程数据序列化失败", ex);
    }
  }

  /**
   * 生成需求文档为 Word
   *
   * @param request 文档生成请求参数
   * @return Word 文件字节数组
   */
  public byte[] generateRequirementDocumentAsBytes(DocumentExportRequest request) {
    return cosmicPrdService.generateWordDocument(request.getOverrideDocumentContent());
  }
}
