package com.excalicode.platform.core.service;

import com.excalicode.platform.common.enums.AiFunctionType;
import com.excalicode.platform.common.exception.BusinessException;
import com.excalicode.platform.core.ai.AiFunctionExecutor;
import com.excalicode.platform.core.api.cosmic.AnalysisResponse;
import com.excalicode.platform.core.api.cosmic.CosmicAnalysisRequest;
import com.excalicode.platform.core.api.cosmic.CosmicProcessBaseResponse;
import com.excalicode.platform.core.api.cosmic.CosmicProcessesResponse;
import com.excalicode.platform.core.api.cosmic.DataGroupAttributeResponse;
import com.excalicode.platform.core.api.cosmic.DocumentExportRequest;
import com.excalicode.platform.core.api.cosmic.DocumentPreviewRequest;
import com.excalicode.platform.core.api.cosmic.FixDuplicateResponse;
import com.excalicode.platform.core.api.cosmic.FunctionalProcess;
import com.excalicode.platform.core.api.cosmic.FunctionalProcessesResponse;
import com.excalicode.platform.core.api.cosmic.ProcessBreakdownRequest;
import com.excalicode.platform.core.api.cosmic.ProcessBreakdownResponse;
import com.excalicode.platform.core.api.cosmic.ProcessTableExportRequest;
import com.excalicode.platform.core.model.cosmic.CosmicProcess;
import com.excalicode.platform.core.model.cosmic.CosmicProcessStep;
import com.excalicode.platform.core.model.cosmic.DuplicateCheckResult;
import com.excalicode.platform.core.model.cosmic.DuplicateItem;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * COSMIC 业务编排服务
 */
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
    private final ExecutorService executorService;

    /**
     * 流式返回需求扩写结果
     *
     * @param originalRequirement 原始需求描述
     * @return SSE 文本片段流，组成扩写后的需求描述
     */
    public Flux<String> streamEnhancedRequirement(String originalRequirement) {
        if (!StringUtils.hasText(originalRequirement)) {
            return Flux.error(new BusinessException("原始需求描述不能为空"));
        }
        return aiFunctionExecutor.streamText(AiFunctionType.COSMIC_REQUIREMENT_ENHANCE, originalRequirement.trim())
                .onErrorMap(error -> {
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
        String userPromptText = buildFunctionalProcessUserPrompt(request.getRequirementDescription(),
                                                                 request.getExpectedProcessCount());
        AiFunctionExecutor.AiFunctionResult<FunctionalProcessesResponse> aiResult =
                aiFunctionExecutor.executeStructured(AiFunctionType.COSMIC_FUNCTIONAL_BREAKDOWN, userPromptText,
                                                     FunctionalProcessesResponse.class);

        FunctionalProcessesResponse result = aiResult.value();
        if (result == null || CollectionUtils.isEmpty(result.getFunctionalProcesses())) {
            throw new BusinessException("AI 拆解未能生成有效的功能过程");
        }
        List<FunctionalProcess> processes = result.getFunctionalProcesses();
        if (CollectionUtils.isEmpty(processes)) {
            throw new BusinessException("AI拆解未返回有效结果");
        }
        return ProcessBreakdownResponse.builder().functionalProcesses(processes).build();
    }

    private String buildFunctionalProcessUserPrompt(String requirementDescription, Integer expectedProcessCount) {
        StringBuilder builder = new StringBuilder();

        builder.append("需求描述如下：")
                .append(System.lineSeparator())
                .append(requirementDescription == null ? "" : requirementDescription.trim());

        if (expectedProcessCount != null && expectedProcessCount > 0) {
            builder.append(System.lineSeparator())
                    .append("请在不遗漏关键业务场景的前提下，尽量将上述需求拆解为约 ")
                    .append(expectedProcessCount)
                    .append(" 个功能过程。");
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
     * @param sheet       工作表
     * @param rowIndex    行索引
     * @param columnIndex 列索引
     * @return true表示应跳过，false表示需要读取
     */
    private boolean shouldSkipMergedCell(Sheet sheet, int rowIndex, int columnIndex) {
        for (CellRangeAddress mergedRegion : sheet.getMergedRegions()) {
            if (mergedRegion.isInRange(rowIndex, columnIndex)) {
                // 如果在合并区域内，只有第一行第一列的单元格不跳过
                return !(rowIndex == mergedRegion.getFirstRow() && columnIndex == mergedRegion.getFirstColumn());
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
        int[] columns = {COLUMN_TRIGGER_EVENT, COLUMN_FUNCTIONAL_PROCESS, COLUMN_SUB_PROCESS, COLUMN_DATA_MOVEMENT,
                COLUMN_DATA_GROUP, COLUMN_DATA_ATTRIBUTES};

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

            String functionalProcess = getCellValueAsString(row.getCell(COLUMN_FUNCTIONAL_PROCESS)).trim();
            if (StringUtils.hasText(functionalProcess)) {
                currentFunctionalProcess = functionalProcess;
            }

            String subProcessDesc = getCellValueAsString(row.getCell(COLUMN_SUB_PROCESS)).trim();
            String dataMovement = getCellValueAsString(row.getCell(COLUMN_DATA_MOVEMENT)).trim();
            String dataGroup = getCellValueAsString(row.getCell(COLUMN_DATA_GROUP)).trim();
            String dataAttributes = getCellValueAsString(row.getCell(COLUMN_DATA_ATTRIBUTES)).trim();

            boolean rowEmpty = !StringUtils.hasText(triggerEvent) && !StringUtils.hasText(
                    functionalProcess) && !StringUtils.hasText(subProcessDesc) && !StringUtils.hasText(
                    dataMovement) && !StringUtils.hasText(dataGroup) && !StringUtils.hasText(dataAttributes);
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
            CosmicProcess process = grouped.computeIfAbsent(key, ignored -> CosmicProcess.builder()
                    .triggerEvent(triggerEventKey)
                    .functionalProcess(functionalProcessKey)
                    .processSteps(new ArrayList<>())
                    .build());

            process.getProcessSteps()
                    .add(CosmicProcessStep.builder()
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
        AiFunctionExecutor.AiFunctionResult<CosmicProcessesResponse> aiResult =
                aiFunctionExecutor.executeStructured(AiFunctionType.COSMIC_ANALYSIS, userPromptText,
                                                     CosmicProcessesResponse.class);
        CosmicProcessesResponse result = aiResult.value();
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
                builder.append(i + 1)
                        .append(". ")
                        .append(process.getDescription() == null ? "" : process.getDescription().trim());
                builder.append(System.lineSeparator());
            }
        }

        return builder.toString();
    }

    /**
     * 生成子过程 V2 (Alpha版本 - 两阶段方法)
     *
     * @param request 需求请求参数
     * @return 分析结果，包含 COSMIC 过程数据
     * @throws BusinessException 业务异常
     */
    public AnalysisResponse analyzeRequirementV2(CosmicAnalysisRequest request) {
        List<FunctionalProcess> functionalProcesses = request.getFunctionalProcesses();

        List<CosmicProcess> baseProcesses = analyzeCosmicProcessesPhase1(functionalProcesses);
        int totalStepsPhase1 = baseProcesses.stream().mapToInt(p -> p.getProcessSteps().size()).sum();
        log.info("阶段1完成, 生成子过程数量: {}", totalStepsPhase1);

        List<CosmicProcess> enrichedProcesses = analyzeCosmicProcessesPhase2Parallel(baseProcesses);
        int totalStepsPhase2 = enrichedProcesses.stream().mapToInt(p -> p.getProcessSteps().size()).sum();
        log.info("阶段2完成, 完整步骤数量: {}", totalStepsPhase2);

        checkAndFixDuplicates(enrichedProcesses);
        log.info("阶段3复检完成");
        return AnalysisResponse.builder().processes(enrichedProcesses).build();
    }

    private List<CosmicProcess> analyzeCosmicProcessesPhase1(List<FunctionalProcess> functionalProcesses) {
        String userPromptText = buildCosmicUserPrompt(functionalProcesses);
        AiFunctionExecutor.AiFunctionResult<CosmicProcessBaseResponse> aiResult =
                aiFunctionExecutor.executeStructured(AiFunctionType.COSMIC_ANALYSIS_PHASE1, userPromptText,
                                                     CosmicProcessBaseResponse.class);
        String response = aiResult.rawResponse();
        log.info("阶段1 AI响应长度: {} 字符", response != null ? response.length() : 0);

        CosmicProcessBaseResponse result = aiResult.value();
        if (result == null || CollectionUtils.isEmpty(result.getProcesses())) {
            throw new BusinessException("阶段1: AI未能生成有效的基础过程");
        }

        return sanitizeCosmicProcesses(result.getProcesses());
    }

    private List<CosmicProcess> analyzeCosmicProcessesPhase2Parallel(List<CosmicProcess> baseProcesses) {
        List<CosmicProcess> processes = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int processIndex = 0; processIndex < baseProcesses.size(); processIndex++) {
            CosmicProcess baseProcess = baseProcesses.get(processIndex);
            List<CosmicProcessStep> baseSteps = baseProcess.getProcessSteps();

            List<CosmicProcessStep> resolvedSteps = new ArrayList<>(Collections.nCopies(baseSteps.size(), null));
            CosmicProcess process = CosmicProcess.builder()
                    .triggerEvent(baseProcess.getTriggerEvent())
                    .functionalProcess(baseProcess.getFunctionalProcess())
                    .processSteps(resolvedSteps)
                    .build();
            processes.add(process);

            for (int stepIndex = 0; stepIndex < baseSteps.size(); stepIndex++) {
                CosmicProcessStep stepBase = baseSteps.get(stepIndex);
                final int finalProcessIndex = processIndex;
                final int finalStepIndex = stepIndex;
                futures.add(CompletableFuture.supplyAsync(() -> generateDataGroupAndAttributes(baseProcess, stepBase),
                                                          executorService)
                                    .thenAccept(resultStep -> processes.get(finalProcessIndex)
                                            .getProcessSteps()
                                            .set(finalStepIndex, resultStep)));
            }
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            allOf.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new BusinessException("阶段2: 生成数据组和数据属性失败: " + cause.getMessage(), cause);
        }

        for (int i = 0; i < processes.size(); i++) {
            CosmicProcess process = processes.get(i);
            if (process.getProcessSteps().stream().anyMatch(Objects::isNull)) {
                throw new BusinessException(String.format("阶段2: 第 %d 个功能过程存在未完成的步骤生成", i + 1));
            }
        }

        return processes;
    }

    private CosmicProcessStep generateDataGroupAndAttributes(CosmicProcess baseProcess, CosmicProcessStep stepBase) {
        DataGroupAttributeResponse response = callPhase2AI(baseProcess, stepBase);

        String dataGroup = trimToEmpty(response.getDataGroup());
        if (!StringUtils.hasText(dataGroup)) {
            throw new BusinessException(
                    String.format("阶段2: 功能过程 \"%s\" 的子过程数据组为空", baseProcess.getFunctionalProcess()));
        }

        String dataAttributes = trimToEmpty(response.getDataAttributes());

        return CosmicProcessStep.builder()
                .subProcessDesc(stepBase.getSubProcessDesc())
                .dataMovementType(stepBase.getDataMovementType())
                .dataGroup(dataGroup)
                .dataAttributes(dataAttributes)
                .build();
    }

    private DataGroupAttributeResponse callPhase2AI(CosmicProcess baseProcess, CosmicProcessStep stepBase) {
        String userPromptText = String.format("触发事件: %s%n功能过程: %s%n子过程描述: %s%n数据移动类型: %s",
                                              baseProcess.getTriggerEvent(), baseProcess.getFunctionalProcess(),
                                              stepBase.getSubProcessDesc(), stepBase.getDataMovementType());
        AiFunctionExecutor.AiFunctionResult<DataGroupAttributeResponse> aiResult =
                aiFunctionExecutor.executeStructured(AiFunctionType.COSMIC_ANALYSIS_PHASE2, userPromptText,
                                                     DataGroupAttributeResponse.class);

        DataGroupAttributeResponse result = aiResult.value();
        if (result == null || !StringUtils.hasText(result.getDataGroup())) {
            throw new BusinessException("阶段2: AI返回的数据组为空");
        }

        return result;
    }

    private void checkAndFixDuplicates(List<CosmicProcess> processes) {
        log.info("开始重复项检测");

        DuplicateCheckResult checkResult = detectDuplicates(processes);

        if (!checkResult.hasDuplicates()) {
            log.info("未检测到重复项");
            return;
        }

        log.warn("检测到重复项 - 子过程描述: {}, 数据组: {}, 数据属性: {}",
                 checkResult.getDuplicateSubProcessDescs().size(), checkResult.getDuplicateDataGroups().size(),
                 checkResult.getDuplicateDataAttributes().size());

        fixDuplicatesInPlace(processes, checkResult);
        log.info("重复项修复完成");
    }

    private DuplicateCheckResult detectDuplicates(List<CosmicProcess> processes) {
        DuplicateCheckResult result = new DuplicateCheckResult();

        Set<String> seenSubProcessDescs = new HashSet<>();
        Set<String> seenDataGroups = new HashSet<>();
        Set<String> seenDataAttributes = new HashSet<>();

        for (int processIndex = 0; processIndex < processes.size(); processIndex++) {
            CosmicProcess process = processes.get(processIndex);
            List<CosmicProcessStep> steps = process.getProcessSteps();
            for (int stepIndex = 0; stepIndex < steps.size(); stepIndex++) {
                CosmicProcessStep step = steps.get(stepIndex);

                String desc = step.getSubProcessDesc();
                if (seenSubProcessDescs.contains(desc)) {
                    result.addDuplicateSubProcessDesc(processIndex, stepIndex, desc);
                } else {
                    seenSubProcessDescs.add(desc);
                }

                String dataGroup = step.getDataGroup();
                if (StringUtils.hasText(dataGroup)) {
                    if (seenDataGroups.contains(dataGroup)) {
                        result.addDuplicateDataGroup(processIndex, stepIndex, dataGroup);
                    } else {
                        seenDataGroups.add(dataGroup);
                    }
                }

                String dataAttributes = step.getDataAttributes();
                if (StringUtils.hasText(dataAttributes)) {
                    if (seenDataAttributes.contains(dataAttributes)) {
                        result.addDuplicateDataAttribute(processIndex, stepIndex, dataAttributes);
                    } else {
                        seenDataAttributes.add(dataAttributes);
                    }
                }
            }
        }

        return result;
    }

    private void fixDuplicatesInPlace(List<CosmicProcess> processes, DuplicateCheckResult checkResult) {
        Set<String> allSubProcessDescs = processes.stream()
                .flatMap(p -> p.getProcessSteps().stream())
                .map(CosmicProcessStep::getSubProcessDesc)
                .collect(Collectors.toSet());

        Set<String> allDataGroups = processes.stream()
                .flatMap(p -> p.getProcessSteps().stream())
                .map(CosmicProcessStep::getDataGroup)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        Set<String> allDataAttributes = processes.stream()
                .flatMap(p -> p.getProcessSteps().stream())
                .map(CosmicProcessStep::getDataAttributes)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        for (DuplicateItem item : checkResult.getDuplicateSubProcessDescs()) {
            CosmicProcess process = processes.get(item.processIndex());
            CosmicProcessStep step = process.getProcessSteps().get(item.stepIndex());
            String fixed = fixSubProcessDesc(process, step, allSubProcessDescs);
            step.setSubProcessDesc(fixed);
            allSubProcessDescs.add(fixed);
            log.info("修复子过程描述 [过程索引{} 步骤索引{}]: \"{}\" → \"{}\"", item.processIndex(), item.stepIndex(),
                     item.value(), fixed);
        }

        for (DuplicateItem item : checkResult.getDuplicateDataGroups()) {
            CosmicProcess process = processes.get(item.processIndex());
            CosmicProcessStep step = process.getProcessSteps().get(item.stepIndex());
            String fixed = fixDataGroup(process, step, allDataGroups);
            step.setDataGroup(fixed);
            allDataGroups.add(fixed);
            log.info("修复数据组 [过程索引{} 步骤索引{}]: \"{}\" → \"{}\"", item.processIndex(), item.stepIndex(),
                     item.value(), fixed);
        }

        for (DuplicateItem item : checkResult.getDuplicateDataAttributes()) {
            CosmicProcess process = processes.get(item.processIndex());
            CosmicProcessStep step = process.getProcessSteps().get(item.stepIndex());
            String fixed = fixDataAttributes(process, step, allDataAttributes);
            step.setDataAttributes(fixed);
            allDataAttributes.add(fixed);
            log.info("修复数据属性 [过程索引{} 步骤索引{}]: \"{}\" → \"{}\"", item.processIndex(), item.stepIndex(),
                     item.value(), fixed);
        }
    }

    private String fixSubProcessDesc(CosmicProcess process, CosmicProcessStep step, Set<String> usedDescs) {
        String userPrompt = String.format("""
                                                  修复类型: subProcessDesc
                                                  原始内容: %s
                                                  原始上下文:
                                                    触发事件: %s
                                                    功能过程: %s
                                                    数据移动类型: %s
                                                  已使用列表:
                                                  %s""", step.getSubProcessDesc(), process.getTriggerEvent(),
                                          process.getFunctionalProcess(), step.getDataMovementType(),
                                          usedDescs.stream().map(d -> "  - " + d).collect(Collectors.joining("\n")));

        return callFixAI(userPrompt);
    }

    private String fixDataGroup(CosmicProcess process, CosmicProcessStep step, Set<String> usedGroups) {
        String userPrompt = String.format("""
                                                  修复类型: dataGroup
                                                  原始内容: %s
                                                  原始上下文:
                                                    触发事件: %s
                                                    功能过程: %s
                                                    子过程描述: %s
                                                    数据移动类型: %s
                                                  已使用列表:
                                                  %s""", step.getDataGroup(), process.getTriggerEvent(),
                                          process.getFunctionalProcess(), step.getSubProcessDesc(),
                                          step.getDataMovementType(),
                                          usedGroups.stream().map(g -> "  - " + g).collect(Collectors.joining("\n")));

        return callFixAI(userPrompt);
    }

    private String fixDataAttributes(CosmicProcess process, CosmicProcessStep step, Set<String> usedAttributes) {
        String userPrompt = String.format("""
                                                  修复类型: dataAttributes
                                                  原始内容: %s
                                                  原始上下文:
                                                    触发事件: %s
                                                    功能过程: %s
                                                    子过程描述: %s
                                                    数据移动类型: %s
                                                  已使用列表:
                                                  %s""", step.getDataAttributes(), process.getTriggerEvent(),
                                          process.getFunctionalProcess(), step.getSubProcessDesc(),
                                          step.getDataMovementType(), usedAttributes.stream()
                                                  .map(a -> "  - " + a)
                                                  .collect(Collectors.joining("\n")));

        return callFixAI(userPrompt);
    }

    private String callFixAI(String userPrompt) {
        AiFunctionExecutor.AiFunctionResult<FixDuplicateResponse> aiResult =
                aiFunctionExecutor.executeStructured(AiFunctionType.COSMIC_FIX_DUPLICATES, userPrompt,
                                                     FixDuplicateResponse.class);

        FixDuplicateResponse result = aiResult.value();
        if (result == null || !StringUtils.hasText(result.getFixed())) {
            throw new BusinessException("修复AI返回的内容为空");
        }

        return result.getFixed();
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

            sanitized.add(CosmicProcess.builder()
                                  .triggerEvent(triggerEvent)
                                  .functionalProcess(functionalProcess)
                                  .processSteps(sanitizedSteps)
                                  .build());
        }

        return sanitized;
    }

    private CosmicProcessStep sanitizeStep(CosmicProcessStep step, String functionalProcess, int stepIndex) {
        // 必填:子过程描述
        String subProcessDesc = trimToEmpty(step.getSubProcessDesc());
        if (!StringUtils.hasText(subProcessDesc)) {
            throw new BusinessException(
                    String.format("功能过程 \"%s\" 的第 %d 个子过程描述为空", functionalProcess, stepIndex + 1));
        }

        // 必填:数据移动类型
        String dataMovementType = normalizeMovementType(step.getDataMovementType(), functionalProcess, stepIndex);

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
                    String.format("功能过程 \"%s\" 的第 %d 个子过程数据移动类型无效: %s", functionalProcess,
                                  stepIndex + 1, value));
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

        String processDescription = sanitized.stream()
                .flatMap(p -> p.getProcessSteps()
                        .stream()
                        .map(step -> String.format("功能过程：%s，子过程：%s", p.getFunctionalProcess(),
                                                   step.getSubProcessDesc())))
                .collect(Collectors.joining("；"));

        String requirementName = request.getRequirementName();
        if (!StringUtils.hasText(requirementName)) {
            throw new BusinessException("需求名称不能为空");
        }
        String userInput =
                String.format("需求名称：%s\n功能过程或子过程描述：%s", requirementName.trim(), processDescription);
        return aiFunctionExecutor.executeText(AiFunctionType.COSMIC_PRD_GENERATION, userInput);
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
