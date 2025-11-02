package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.cosmic.AnalysisResponse;
import com.excalicode.platform.core.api.cosmic.CosmicAnalysisRequest;
import com.excalicode.platform.core.api.cosmic.DocumentExportRequest;
import com.excalicode.platform.core.api.cosmic.DocumentPreviewRequest;
import com.excalicode.platform.core.api.cosmic.ProcessBreakdownRequest;
import com.excalicode.platform.core.api.cosmic.ProcessBreakdownResponse;
import com.excalicode.platform.core.api.cosmic.ProcessTableExportRequest;
import com.excalicode.platform.core.api.cosmic.RequirementEnhanceRequest;
import com.excalicode.platform.core.service.CosmicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * COSMIC 需求管理控制器
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class CosmicController {

    private final CosmicService cosmicService;

    /**
     * 扩写需求描述
     */
    @PostMapping(value = "/requirement/enhance", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> enhanceRequirement(@RequestBody @Valid RequirementEnhanceRequest request) {
        return cosmicService.streamEnhancedRequirement(request.getOriginalRequirement());
    }

    /**
     * 生成功能过程
     */
    @PostMapping("/process/breakdown")
    public ResponseEntity<ProcessBreakdownResponse> breakdownProcess(@RequestBody @Valid ProcessBreakdownRequest request) {
        ProcessBreakdownResponse result = cosmicService.breakdownProcess(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 导入功能过程
     */
    @PostMapping("/cosmic/process/import")
    public ResponseEntity<ProcessBreakdownResponse> importFunctionalProcesses(@RequestParam("file") MultipartFile file) {
        ProcessBreakdownResponse result = cosmicService.importFunctionalProcesses(file);
        return ResponseEntity.ok(result);
    }

    /**
     * 导入 COSMIC 子过程
     */
    @PostMapping("/cosmic/subprocess/import")
    public ResponseEntity<AnalysisResponse> importCosmicProcesses(@RequestParam("file") MultipartFile file) {
        AnalysisResponse result = cosmicService.importCosmicProcesses(file);
        return ResponseEntity.ok(result);
    }

    /**
     * 生成子过程 V1 (稳定版本)
     * 使用一次性生成所有字段的方式。
     */
    @PostMapping("/cosmic/analyze")
    public ResponseEntity<AnalysisResponse> analyzeRequirement(@RequestBody @Valid CosmicAnalysisRequest request) {
        AnalysisResponse result = cosmicService.analyzeRequirement(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 生成子过程 V2 (Alpha版本 - 两阶段方法)
     * 阶段1: 生成基础字段(触发事件、功能过程、子过程描述、数据移动类型)
     * 阶段2: 多线程并发生成数据组和数据属性,带重试机制
     * 阶段3: 自动检测并修复重复项,最多3轮
     * 解决大量功能过程时的截断问题(20个功能过程只生成10个) 并发处理提升性能 自动修复重复项,提高质量
     */
    @PostMapping("/cosmic/analyze-v2")
    public ResponseEntity<AnalysisResponse> analyzeRequirementV2(@RequestBody @Valid CosmicAnalysisRequest request) {
        AnalysisResponse result = cosmicService.analyzeRequirementV2(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 导出子功能过程表格为 Excel 文件
     */
    @PostMapping("/cosmic/table/export")
    public ResponseEntity<Resource> exportProcessTable(@RequestBody @Valid ProcessTableExportRequest request) {
        byte[] excelData = cosmicService.exportProcessTableAsBytes(request);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
        String filename = URLEncoder.encode("COSMIC_" + timestamp + ".xlsx", StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(new ByteArrayResource(excelData));
    }

    /**
     * 生成文档预览内容
     */
    @PostMapping("/cosmic/documents/preview")
    public ResponseEntity<String> generateDocumentPreview(@RequestBody @Valid DocumentPreviewRequest request) {
        String content = cosmicService.generateDocumentPreview(request);
        return ResponseEntity.ok(content);
    }

    /**
     * 导出需求文档为 Word 文件
     */
    @PostMapping("/cosmic/documents/export")
    public ResponseEntity<Resource> exportRequirementDocument(@RequestBody @Valid DocumentExportRequest request) {
        byte[] docxData = cosmicService.generateRequirementDocumentAsBytes(request);
        String filename = URLEncoder.encode("需求文档.docx", StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(new ByteArrayResource(docxData));
    }

}
