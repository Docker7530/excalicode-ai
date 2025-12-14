package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.task.TaskAssigneeResponse;
import com.excalicode.platform.core.api.task.TaskAssignmentRequest;
import com.excalicode.platform.core.api.task.TaskBatchDetailResponse;
import com.excalicode.platform.core.api.task.TaskBatchSummaryResponse;
import com.excalicode.platform.core.api.task.TaskDraftResponse;
import com.excalicode.platform.core.api.task.TaskPublishRequest;
import com.excalicode.platform.core.api.task.TaskResponse;
import com.excalicode.platform.core.api.task.TaskStatusUpdateRequest;
import com.excalicode.platform.core.entity.SysUser;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.task.TaskStatus;
import com.excalicode.platform.core.service.TaskManagementService;
import com.excalicode.platform.core.service.entity.SysUserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 任务批次与子任务管理控制器 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

  private final TaskManagementService taskManagementService;
  private final SysUserService sysUserService;

  /** 导入 Excel 模板并返回草稿 */
  @PostMapping("/admin/task-batches/import")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TaskDraftResponse>> importBatch(
      @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(taskManagementService.parseTaskTemplate(file));
  }

  /** 发布任务批次 */
  @PostMapping("/admin/task-batches")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TaskBatchDetailResponse> publishBatch(
      @RequestBody @Valid TaskPublishRequest request) {
    SysUser currentUser = requireCurrentUser();
    TaskBatchDetailResponse detail = taskManagementService.publishTaskBatch(request, currentUser);
    return ResponseEntity.ok(detail);
  }

  /** 管理员查看批次列表 */
  @GetMapping("/admin/task-batches")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TaskBatchSummaryResponse>> listBatches() {
    return ResponseEntity.ok(taskManagementService.listAllBatches());
  }

  /** 管理员查看批次详情 */
  @GetMapping("/admin/task-batches/{batchId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TaskBatchDetailResponse> getBatchDetail(@PathVariable Long batchId) {
    return ResponseEntity.ok(taskManagementService.getBatchDetail(batchId));
  }

  /** 更新子任务执行人 */
  @PutMapping("/admin/task-batches/{batchId}/tasks/{taskId}/assignee")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TaskResponse> updateTaskAssignee(
      @PathVariable Long batchId,
      @PathVariable Long taskId,
      @RequestBody @Valid TaskAssignmentRequest request) {
    TaskResponse response =
        taskManagementService.updateTaskAssignee(taskId, request.getAssigneeId());
    if (!batchId.equals(response.getBatchId())) {
      throw new BusinessException("任务批次信息不匹配");
    }
    return ResponseEntity.ok(response);
  }

  /** 可分配用户列表 */
  @GetMapping("/admin/task-assignees")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TaskAssigneeResponse>> listAssignableUsers() {
    return ResponseEntity.ok(taskManagementService.listAssignableUsers());
  }

  /** 当前用户参与的大任务列表 */
  @GetMapping("/tasks/my")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<List<TaskBatchSummaryResponse>> listMyTaskBatches() {
    SysUser currentUser = requireCurrentUser();
    return ResponseEntity.ok(taskManagementService.listBatchesForAssignee(currentUser.getId()));
  }

  /** 查看某批次下分配给当前用户的任务 */
  @GetMapping("/tasks/my/{batchId}")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<TaskBatchDetailResponse> getMyBatchDetail(@PathVariable Long batchId) {
    SysUser currentUser = requireCurrentUser();
    return ResponseEntity.ok(
        taskManagementService.getBatchDetailForAssignee(batchId, currentUser.getId()));
  }

  /** 更新子任务状态 */
  @PatchMapping("/tasks/{taskId}/status")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<TaskResponse> updateTaskStatus(
      @PathVariable Long taskId, @RequestBody @Valid TaskStatusUpdateRequest request) {
    SysUser currentUser = requireCurrentUser();
    TaskStatus status = resolveStatus(request.getStatus());
    boolean allowOverride = "ADMIN".equalsIgnoreCase(currentUser.getRole());
    TaskResponse response =
        taskManagementService.updateTaskStatus(taskId, status, currentUser, allowOverride);
    return ResponseEntity.ok(response);
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

  private TaskStatus resolveStatus(String status) {
    try {
      return TaskStatus.fromValue(status);
    } catch (IllegalArgumentException ex) {
      throw new BusinessException("不支持的任务状态: " + status);
    }
  }
}
