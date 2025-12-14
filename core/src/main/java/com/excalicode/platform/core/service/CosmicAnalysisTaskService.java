package com.excalicode.platform.core.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.excalicode.platform.core.api.cosmic.AnalysisResponse;
import com.excalicode.platform.core.api.cosmic.CosmicAnalysisRequest;
import com.excalicode.platform.core.api.cosmic.CosmicAnalysisTaskResponse;
import com.excalicode.platform.core.api.cosmic.FunctionalProcess;
import com.excalicode.platform.core.entity.CosmicAnalysisTask;
import com.excalicode.platform.core.entity.SysUser;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.mapper.CosmicAnalysisTaskMapper;
import com.excalicode.platform.core.model.cosmic.CosmicAnalysisTaskStatus;
import com.excalicode.platform.core.model.cosmic.CosmicProcess;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** COSMIC 子过程异步任务服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CosmicAnalysisTaskService {

  private static final int MAX_TASK_LIST_SIZE = 30;

  private final CosmicAnalysisTaskMapper cosmicAnalysisTaskMapper;
  private final CosmicService cosmicService;
  private final ObjectMapper objectMapper;
  private final ExecutorService executorService;

  /** 启动时重新派发未完成任务，避免重启后丢失执行 */
  @PostConstruct
  public void resumePendingTasks() {
    List<String> targetStatuses =
        List.of(CosmicAnalysisTaskStatus.PENDING.name(), CosmicAnalysisTaskStatus.RUNNING.name());
    List<CosmicAnalysisTask> pendingTasks =
        cosmicAnalysisTaskMapper.selectList(
            Wrappers.<CosmicAnalysisTask>lambdaQuery()
                .in(CosmicAnalysisTask::getStatus, targetStatuses)
                .eq(CosmicAnalysisTask::getDeleted, 0));
    if (CollectionUtils.isEmpty(pendingTasks)) {
      return;
    }
    pendingTasks.forEach(task -> dispatchAsync(task.getId()));
  }

  /** 提交子过程生成任务 */
  public CosmicAnalysisTaskResponse submitTask(CosmicAnalysisRequest request, SysUser user) {
    if (user == null) {
      throw new BusinessException("未检测到登录用户，无法提交任务");
    }
    CosmicAnalysisRequest sanitized = sanitizeRequest(request);
    String payload = serializePayload(sanitized);

    CosmicAnalysisTask task = new CosmicAnalysisTask();
    task.setUserId(user.getId());
    task.setUsername(user.getUsername());
    task.setStatus(CosmicAnalysisTaskStatus.PENDING.name());
    task.setRequestPayload(payload);
    task.setErrorMessage(null);
    task.setDeleted(0);
    cosmicAnalysisTaskMapper.insert(task);

    dispatchAsync(task.getId());
    return buildResponse(task, false);
  }

  /** 当前用户的任务列表（默认取最近一批即可） */
  public List<CosmicAnalysisTaskResponse> listTasks(Long userId) {
    if (userId == null) {
      return Collections.emptyList();
    }
    List<CosmicAnalysisTask> tasks =
        cosmicAnalysisTaskMapper.selectList(
            Wrappers.<CosmicAnalysisTask>lambdaQuery()
                .eq(CosmicAnalysisTask::getUserId, userId)
                .eq(CosmicAnalysisTask::getDeleted, 0)
                .orderByDesc(CosmicAnalysisTask::getCreatedTime)
                .last("limit " + MAX_TASK_LIST_SIZE));
    if (CollectionUtils.isEmpty(tasks)) {
      return Collections.emptyList();
    }
    return tasks.stream().map(task -> buildResponse(task, false)).toList();
  }

  /** 查询单个任务详情（包含结果） */
  public CosmicAnalysisTaskResponse getTaskDetail(Long taskId, Long userId) {
    if (taskId == null || userId == null) {
      throw new BusinessException("任务信息缺失");
    }
    CosmicAnalysisTask task =
        cosmicAnalysisTaskMapper.selectOne(
            Wrappers.<CosmicAnalysisTask>lambdaQuery()
                .eq(CosmicAnalysisTask::getId, taskId)
                .eq(CosmicAnalysisTask::getUserId, userId)
                .eq(CosmicAnalysisTask::getDeleted, 0));
    if (task == null) {
      throw new BusinessException("未找到对应的子过程任务");
    }
    return buildResponse(task, true);
  }

  private void dispatchAsync(Long taskId) {
    if (taskId == null) {
      return;
    }
    executorService.submit(() -> processTask(taskId));
  }

  private void processTask(Long taskId) {
    CosmicAnalysisTask task = cosmicAnalysisTaskMapper.selectById(taskId);
    if (task == null || Objects.equals(task.getDeleted(), 1)) {
      return;
    }
    CosmicAnalysisTaskStatus currentStatus = resolveStatus(task.getStatus());
    if (currentStatus == CosmicAnalysisTaskStatus.SUCCEEDED) {
      return;
    }

    markRunning(taskId);

    CosmicAnalysisRequest analysisRequest;
    try {
      analysisRequest = deserializeRequest(task.getRequestPayload());
    } catch (Exception ex) {
      log.warn("子过程任务入参解析失败, taskId={}", taskId, ex);
      saveFailure(taskId, "任务入参解析失败: " + ex.getMessage());
      return;
    }

    try {
      AnalysisResponse result = cosmicService.analyzeRequirement(analysisRequest);
      saveSuccess(taskId, result);
    } catch (Exception ex) {
      log.error("子过程任务执行失败, taskId={}", taskId, ex);
      saveFailure(taskId, ex.getMessage());
    }
  }

  private void markRunning(Long taskId) {
    CosmicAnalysisTask update = new CosmicAnalysisTask();
    update.setId(taskId);
    update.setStatus(CosmicAnalysisTaskStatus.RUNNING.name());
    update.setStartedTime(LocalDateTime.now());
    update.setErrorMessage(null);
    update.setFinishedTime(null);
    cosmicAnalysisTaskMapper.updateById(update);
  }

  private void saveSuccess(Long taskId, AnalysisResponse result) {
    CosmicAnalysisTask update = new CosmicAnalysisTask();
    update.setId(taskId);
    update.setStatus(CosmicAnalysisTaskStatus.SUCCEEDED.name());
    update.setResponsePayload(serializeResult(result));
    update.setFinishedTime(LocalDateTime.now());
    update.setErrorMessage(null);
    cosmicAnalysisTaskMapper.updateById(update);
  }

  private void saveFailure(Long taskId, String message) {
    CosmicAnalysisTask update = new CosmicAnalysisTask();
    update.setId(taskId);
    update.setStatus(CosmicAnalysisTaskStatus.FAILED.name());
    update.setFinishedTime(LocalDateTime.now());
    update.setErrorMessage(trimToLength(message, 4000));
    cosmicAnalysisTaskMapper.updateById(update);
  }

  private String trimToLength(String message, int maxLength) {
    if (!StringUtils.hasText(message)) {
      return null;
    }
    String trimmed = message.trim();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }

  private CosmicAnalysisTaskResponse buildResponse(
      CosmicAnalysisTask task, boolean includeResultPayload) {
    CosmicAnalysisTaskStatus status = resolveStatus(task.getStatus());
    CosmicAnalysisTaskResponse.CosmicAnalysisTaskResponseBuilder builder =
        CosmicAnalysisTaskResponse.builder()
            .taskId(task.getId())
            .status(status)
            .statusLabel(status.getLabel())
            .errorMessage(task.getErrorMessage())
            .createdTime(task.getCreatedTime())
            .startedTime(task.getStartedTime())
            .finishedTime(task.getFinishedTime());

    CosmicAnalysisRequest request = safelyParseRequest(task.getRequestPayload());
    if (request != null) {
      builder.functionalProcesses(request.getFunctionalProcesses());
    }

    AnalysisResponse parsedResponse = null;
    if (status == CosmicAnalysisTaskStatus.SUCCEEDED
        && StringUtils.hasText(task.getResponsePayload())) {
      parsedResponse = safelyParseResult(task.getResponsePayload());
    }
    if (parsedResponse != null && !CollectionUtils.isEmpty(parsedResponse.getProcesses())) {
      List<CosmicProcess> processes = parsedResponse.getProcesses();
      builder.processCount(processes.size());
      if (includeResultPayload) {
        builder.processes(processes);
      }
    }

    return builder.build();
  }

  private CosmicAnalysisTaskStatus resolveStatus(String status) {
    try {
      return CosmicAnalysisTaskStatus.fromValue(status);
    } catch (Exception ex) {
      log.warn("未知的子过程任务状态: {}", status);
      return CosmicAnalysisTaskStatus.PENDING;
    }
  }

  private String serializePayload(CosmicAnalysisRequest request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (JsonProcessingException ex) {
      log.error("子过程任务入参序列化失败", ex);
      throw new BusinessException("子过程任务序列化失败: " + ex.getMessage());
    }
  }

  private String serializeResult(AnalysisResponse response) {
    try {
      return objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException ex) {
      log.error("子过程任务结果序列化失败", ex);
      throw new BusinessException("子过程结果序列化失败: " + ex.getMessage());
    }
  }

  private CosmicAnalysisRequest sanitizeRequest(CosmicAnalysisRequest request) {
    if (request == null || CollectionUtils.isEmpty(request.getFunctionalProcesses())) {
      throw new BusinessException("功能过程列表不能为空");
    }
    List<FunctionalProcess> sanitized =
        sanitizeFunctionalProcesses(request.getFunctionalProcesses());
    return CosmicAnalysisRequest.builder().functionalProcesses(sanitized).build();
  }

  private List<FunctionalProcess> sanitizeFunctionalProcesses(List<FunctionalProcess> processes) {
    if (CollectionUtils.isEmpty(processes)) {
      throw new BusinessException("功能过程列表不能为空");
    }
    List<FunctionalProcess> sanitized = new ArrayList<>(processes.size());
    for (int i = 0; i < processes.size(); i++) {
      FunctionalProcess process = processes.get(i);
      String description = process == null ? "" : process.getDescription();
      if (!StringUtils.hasText(description)) {
        throw new BusinessException("第 " + (i + 1) + " 个功能过程描述为空");
      }
      sanitized.add(FunctionalProcess.builder().description(description.trim()).build());
    }
    return sanitized;
  }

  private CosmicAnalysisRequest deserializeRequest(String payload) throws JsonProcessingException {
    return objectMapper.readValue(payload, CosmicAnalysisRequest.class);
  }

  private CosmicAnalysisRequest safelyParseRequest(String payload) {
    if (!StringUtils.hasText(payload)) {
      return null;
    }
    try {
      return deserializeRequest(payload);
    } catch (Exception ex) {
      log.warn("子过程任务入参反序列化失败", ex);
      return null;
    }
  }

  private AnalysisResponse safelyParseResult(String payload) {
    if (!StringUtils.hasText(payload)) {
      return null;
    }
    try {
      return objectMapper.readValue(payload, AnalysisResponse.class);
    } catch (Exception ex) {
      log.warn("子过程任务结果反序列化失败", ex);
      return null;
    }
  }
}
