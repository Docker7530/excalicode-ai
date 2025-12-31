package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.cosmic.AnalysisResponse;
import com.excalicode.platform.core.api.cosmic.CosmicAnalysisRequest;
import com.excalicode.platform.core.api.cosmic.CosmicAnalysisTaskResponse;
import com.excalicode.platform.core.api.cosmic.DocumentExportRequest;
import com.excalicode.platform.core.api.cosmic.DocumentPreviewRequest;
import com.excalicode.platform.core.api.cosmic.ProcessBreakdownRequest;
import com.excalicode.platform.core.api.cosmic.ProcessBreakdownResponse;
import com.excalicode.platform.core.api.cosmic.ProcessTableExportRequest;
import com.excalicode.platform.core.api.cosmic.RequirementEnhanceRequest;
import com.excalicode.platform.core.api.cosmic.SequenceDiagramRequest;
import com.excalicode.platform.core.entity.SysUser;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.service.CosmicAnalysisTaskService;
import com.excalicode.platform.core.service.CosmicService;
import com.excalicode.platform.core.service.entity.SysUserService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/** COSMIC 需求管理控制器 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class CosmicController {

  private final CosmicService cosmicService;
  private final CosmicAnalysisTaskService cosmicAnalysisTaskService;
  private final SysUserService sysUserService;

  /** 扩写需求描述 */
  @PostMapping(value = "/requirement/enhance", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> enhanceRequirement(@RequestBody @Valid RequirementEnhanceRequest request) {
    return cosmicService.streamEnhancedRequirement(request);
  }

  /** 生成功能过程 */
  @PostMapping("/process/breakdown")
  public ResponseEntity<ProcessBreakdownResponse> breakdownProcess(
      @RequestBody @Valid ProcessBreakdownRequest request) {
    ProcessBreakdownResponse result = cosmicService.breakdownProcess(request);
    return ResponseEntity.ok(result);
  }

  /** 导入功能过程 */
  @PostMapping("/cosmic/process/import")
  public ResponseEntity<ProcessBreakdownResponse> importFunctionalProcesses(
      @RequestParam("file") MultipartFile file) {
    ProcessBreakdownResponse result = cosmicService.importFunctionalProcesses(file);
    return ResponseEntity.ok(result);
  }

  /** 导入 COSMIC 子过程 */
  @PostMapping("/cosmic/subprocess/import")
  public ResponseEntity<AnalysisResponse> importCosmicProcesses(
      @RequestParam("file") MultipartFile file) {
    AnalysisResponse result = cosmicService.importCosmicProcesses(file);
    return ResponseEntity.ok(result);
  }

  /** 生成子过程：使用一次性生成所有字段的方式。 */
  @PostMapping("/cosmic/analyze")
  public ResponseEntity<AnalysisResponse> analyzeRequirement(
      @RequestBody @Valid CosmicAnalysisRequest request) {
    AnalysisResponse result = cosmicService.analyzeRequirement(request);
    return ResponseEntity.ok(result);
  }

  /** 异步生成子过程：提交任务 */
  @PostMapping("/cosmic/analyze/task")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<CosmicAnalysisTaskResponse> submitAnalysisTask(
      @RequestBody @Valid CosmicAnalysisRequest request) {
    SysUser currentUser = requireCurrentUser();
    CosmicAnalysisTaskResponse response =
        cosmicAnalysisTaskService.submitTask(request, currentUser);
    return ResponseEntity.ok(response);
  }

  /** 异步生成子过程：当前用户任务列表 */
  @GetMapping("/cosmic/analyze/tasks")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<List<CosmicAnalysisTaskResponse>> listAnalysisTasks() {
    SysUser currentUser = requireCurrentUser();
    List<CosmicAnalysisTaskResponse> tasks =
        cosmicAnalysisTaskService.listTasks(currentUser.getId());
    return ResponseEntity.ok(tasks);
  }

  /** 异步生成子过程：任务详情 */
  @GetMapping("/cosmic/analyze/tasks/{taskId}")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<CosmicAnalysisTaskResponse> getAnalysisTaskDetail(
      @PathVariable Long taskId) {
    SysUser currentUser = requireCurrentUser();
    CosmicAnalysisTaskResponse detail =
        cosmicAnalysisTaskService.getTaskDetail(taskId, currentUser.getId());
    return ResponseEntity.ok(detail);
  }

  /** 导出子功能过程表格为 Excel 文件 */
  @PostMapping("/cosmic/table/export")
  public ResponseEntity<Resource> exportProcessTable(
      @RequestBody @Valid ProcessTableExportRequest request) {
    byte[] excelData = cosmicService.exportProcessTableAsBytes(request);
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
    String filename = URLEncoder.encode("COSMIC_" + timestamp + ".xlsx", StandardCharsets.UTF_8);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .body(new ByteArrayResource(excelData));
  }

  /** 生成文档预览内容 */
  @PostMapping("/cosmic/documents/preview")
  public ResponseEntity<String> generateDocumentPreview(
      @RequestBody @Valid DocumentPreviewRequest request) {
    String content = cosmicService.generateDocumentPreview(request);
    return ResponseEntity.ok(content);
  }

  /** 生成时序图（Mermaid） */
  @PostMapping("/cosmic/sequence-diagram")
  public ResponseEntity<String> generateSequenceDiagram(
      @RequestBody @Valid SequenceDiagramRequest request) {
    String diagram = cosmicService.generateSequenceDiagram(request);
    return ResponseEntity.ok(diagram);
  }

  /** 锐评大师：上传 COSMIC 子过程表格，流式输出锐评结果 */
  @PostMapping(
      value = "/cosmic/estimate",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> streamCosmicEstimate(@RequestParam("file") MultipartFile file) {
    return cosmicService.streamCosmicEstimate(file);
  }

  /** 导出需求文档为 Word 文件 */
  @PostMapping("/cosmic/documents/export")
  public ResponseEntity<Resource> exportRequirementDocument(
      @RequestBody @Valid DocumentExportRequest request) {
    byte[] docxData = cosmicService.generateRequirementDocumentAsBytes(request);
    String filename = URLEncoder.encode("需求文档.docx", StandardCharsets.UTF_8);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
        .body(new ByteArrayResource(docxData));
  }

  private SysUser requireCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !StringUtils.hasText(authentication.getName())) {
      throw new BusinessException("未检测到登录用户");
    }
    SysUser user = sysUserService.findByUsername(authentication.getName());
    if (user == null) {
      throw new BusinessException("当前登录用户不存在");
    }
    return user;
  }
}
