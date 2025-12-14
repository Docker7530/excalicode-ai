package com.excalicode.platform.core.service;

import com.excalicode.platform.core.api.task.TaskAssigneeResponse;
import com.excalicode.platform.core.api.task.TaskBatchDetailResponse;
import com.excalicode.platform.core.api.task.TaskBatchSummaryResponse;
import com.excalicode.platform.core.api.task.TaskDraftResponse;
import com.excalicode.platform.core.api.task.TaskPublishRequest;
import com.excalicode.platform.core.api.task.TaskResponse;
import com.excalicode.platform.core.entity.ProjectTask;
import com.excalicode.platform.core.entity.ProjectTaskBatch;
import com.excalicode.platform.core.entity.SysUser;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.task.TaskStatus;
import com.excalicode.platform.core.service.entity.ProjectTaskBatchService;
import com.excalicode.platform.core.service.entity.ProjectTaskService;
import com.excalicode.platform.core.service.entity.SysUserService;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 任务管理服务，负责批次/任务的导入、发布与维护 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskManagementService {

  private static final String TITLE_HEADER = "需求标题";
  private static final String DESCRIPTION_HEADER = "需求描述";
  private static final String WORKLOAD_HEADER = "计入工作量(人天)";

  private final ProjectTaskBatchService projectTaskBatchService;
  private final ProjectTaskService projectTaskService;
  private final SysUserService sysUserService;

  /** 解析 Excel 任务模板 */
  public List<TaskDraftResponse> parseTaskTemplate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("请上传 Excel 模板");
    }
    String filename = file.getOriginalFilename();
    if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
      throw new BusinessException("仅支持 .xlsx 或 .xls 格式的 Excel 文件");
    }

    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(inputStream)) {
      Sheet sheet = workbook.getSheetAt(0);
      if (sheet == null) {
        throw new BusinessException("Excel 缺少有效的工作表");
      }
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) {
        throw new BusinessException("Excel 缺少表头行");
      }
      Map<String, Integer> columnIndexMap = resolveColumnIndex(headerRow);
      int lastRowNum = sheet.getLastRowNum();
      List<TaskDraftResponse> drafts = new ArrayList<>();
      for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
          continue;
        }
        String title = getCellString(row.getCell(columnIndexMap.get(TITLE_HEADER)));
        String description = getCellString(row.getCell(columnIndexMap.get(DESCRIPTION_HEADER)));
        String workloadRaw = getCellString(row.getCell(columnIndexMap.get(WORKLOAD_HEADER)));

        if (!StringUtils.hasText(title)
            && !StringUtils.hasText(description)
            && !StringUtils.hasText(workloadRaw)) {
          continue;
        }
        if (!StringUtils.hasText(title)) {
          throw new BusinessException("第 " + (rowIndex + 1) + " 行任务标题不能为空");
        }
        if (!StringUtils.hasText(description)) {
          throw new BusinessException("第 " + (rowIndex + 1) + " 行任务描述不能为空");
        }
        BigDecimal workload = parseWorkload(workloadRaw, rowIndex);

        TaskDraftResponse draft = new TaskDraftResponse();
        draft.setRowIndex(rowIndex + 1);
        draft.setTitle(title.trim());
        draft.setDescription(description.trim());
        draft.setWorkloadManDay(workload);
        drafts.add(draft);
      }
      if (drafts.isEmpty()) {
        throw new BusinessException("Excel 未包含有效任务数据");
      }
      return drafts;
    } catch (BusinessException ex) {
      throw ex;
    } catch (IOException ex) {
      log.error("读取 Excel 模板失败", ex);
      throw new BusinessException("读取 Excel 文件失败: " + ex.getMessage());
    } catch (Exception ex) {
      log.error("解析任务模板异常", ex);
      throw new BusinessException("解析 Excel 失败: " + ex.getMessage());
    }
  }

  /** 发布任务批次 */
  @Transactional(rollbackFor = Exception.class)
  public TaskBatchDetailResponse publishTaskBatch(TaskPublishRequest request, SysUser creator) {
    if (request == null || CollectionUtils.isEmpty(request.getTasks())) {
      throw new BusinessException("任务列表不能为空");
    }
    if (!StringUtils.hasText(request.getBatchTitle())) {
      throw new BusinessException("批次标题不能为空");
    }

    Set<Long> assigneeIds =
        request.getTasks().stream()
            .map(TaskPublishRequest.TaskPublishItem::getAssigneeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Long, SysUser> assigneeMap = loadAssignableUsers(assigneeIds);

    for (TaskPublishRequest.TaskPublishItem item : request.getTasks()) {
      if (item.getAssigneeId() == null) {
        throw new BusinessException("请为行号 " + item.getRowIndex() + " 的任务选择执行人");
      }
      SysUser assignee = assigneeMap.get(item.getAssigneeId());
      if (assignee == null) {
        throw new BusinessException("找不到执行人，行号 " + item.getRowIndex());
      }
      if (!"USER".equalsIgnoreCase(assignee.getRole())) {
        throw new BusinessException("执行人必须为 USER 角色，行号 " + item.getRowIndex());
      }
      if (item.getWorkloadManDay() == null
          || item.getWorkloadManDay().compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException("工作量必须大于 0，人天行号 " + item.getRowIndex());
      }
    }

    LocalDateTime now = LocalDateTime.now();
    ProjectTaskBatch batch = new ProjectTaskBatch();
    batch.setTitle(request.getBatchTitle().trim());
    batch.setDescription(
        StringUtils.hasText(request.getBatchDescription())
            ? request.getBatchDescription().trim()
            : null);
    batch.setCreatedBy(creator.getId());
    batch.setPublishedTime(now);
    projectTaskBatchService.save(batch);

    List<ProjectTask> tasks = new ArrayList<>();
    for (TaskPublishRequest.TaskPublishItem item : request.getTasks()) {
      ProjectTask task = new ProjectTask();
      task.setBatchId(batch.getId());
      task.setTitle(item.getTitle().trim());
      task.setDescription(item.getDescription().trim());
      task.setWorkloadManDay(item.getWorkloadManDay().setScale(2, RoundingMode.HALF_UP));
      task.setStatus(TaskStatus.NOT_STARTED.name());
      task.setAssigneeId(item.getAssigneeId());
      task.setPublishedTime(now);
      task.setCreatedBy(creator.getId());
      tasks.add(task);
    }
    projectTaskService.saveBatch(tasks);

    Map<Long, SysUser> userMap = new HashMap<>(assigneeMap);
    userMap.put(creator.getId(), creator);
    return buildBatchDetailResponse(batch, tasks, userMap);
  }

  /** 管理员查看所有批次 */
  public List<TaskBatchSummaryResponse> listAllBatches() {
    List<ProjectTaskBatch> batches =
        projectTaskBatchService
            .lambdaQuery()
            .orderByDesc(ProjectTaskBatch::getPublishedTime)
            .list();
    if (CollectionUtils.isEmpty(batches)) {
      return List.of();
    }
    Map<Long, List<ProjectTask>> taskMap =
        loadTasksByBatch(batches.stream().map(ProjectTaskBatch::getId).toList());
    Map<Long, SysUser> userMap = loadUsers(resolveUserIdsFromBatches(batches, taskMap));
    return batches.stream()
        .map(
            batch ->
                buildBatchSummary(batch, taskMap.getOrDefault(batch.getId(), List.of()), userMap))
        .collect(Collectors.toList());
  }

  /** 管理员查看批次详情 */
  public TaskBatchDetailResponse getBatchDetail(Long batchId) {
    ProjectTaskBatch batch = projectTaskBatchService.getById(batchId);
    if (batch == null) {
      throw new BusinessException("任务批次不存在");
    }
    List<ProjectTask> tasks =
        projectTaskService
            .lambdaQuery()
            .eq(ProjectTask::getBatchId, batchId)
            .orderByAsc(ProjectTask::getId)
            .list();
    Map<Long, SysUser> userMap = loadUsers(resolveUserIdsFromTasks(tasks, batch.getCreatedBy()));
    return buildBatchDetailResponse(batch, tasks, userMap);
  }

  /** 用户查看自己参与的批次 */
  public List<TaskBatchSummaryResponse> listBatchesForAssignee(Long assigneeId) {
    List<ProjectTask> tasks =
        projectTaskService.lambdaQuery().eq(ProjectTask::getAssigneeId, assigneeId).list();
    if (CollectionUtils.isEmpty(tasks)) {
      return List.of();
    }
    Set<Long> batchIds = tasks.stream().map(ProjectTask::getBatchId).collect(Collectors.toSet());
    List<ProjectTaskBatch> batches =
        projectTaskBatchService
            .lambdaQuery()
            .in(ProjectTaskBatch::getId, batchIds)
            .orderByDesc(ProjectTaskBatch::getPublishedTime)
            .list();
    Map<Long, List<ProjectTask>> taskMap =
        tasks.stream().collect(Collectors.groupingBy(ProjectTask::getBatchId));
    Map<Long, SysUser> userMap = loadUsers(resolveUserIdsFromBatches(batches, taskMap));
    return batches.stream()
        .map(
            batch ->
                buildUserBatchSummary(
                    batch, taskMap.getOrDefault(batch.getId(), List.of()), assigneeId, userMap))
        .collect(Collectors.toList());
  }

  /** 用户查看某批次下自己的任务 */
  public TaskBatchDetailResponse getBatchDetailForAssignee(Long batchId, Long assigneeId) {
    ProjectTaskBatch batch = projectTaskBatchService.getById(batchId);
    if (batch == null) {
      throw new BusinessException("任务批次不存在");
    }
    List<ProjectTask> tasks =
        projectTaskService
            .lambdaQuery()
            .eq(ProjectTask::getBatchId, batchId)
            .eq(ProjectTask::getAssigneeId, assigneeId)
            .orderByAsc(ProjectTask::getId)
            .list();
    Map<Long, SysUser> userMap = loadUsers(resolveUserIdsFromTasks(tasks, batch.getCreatedBy()));
    return buildBatchDetailResponse(batch, tasks, userMap);
  }

  /** 更新任务执行人 */
  @Transactional(rollbackFor = Exception.class)
  public TaskResponse updateTaskAssignee(Long taskId, Long assigneeId) {
    ProjectTask task = projectTaskService.getById(taskId);
    if (task == null) {
      throw new BusinessException("任务不存在");
    }
    SysUser assignee = sysUserService.getById(assigneeId);
    if (assignee == null) {
      throw new BusinessException("执行人不存在");
    }
    if (!"USER".equalsIgnoreCase(assignee.getRole())) {
      throw new BusinessException("执行人必须为 USER 角色");
    }
    task.setAssigneeId(assigneeId);
    task.setStatus(TaskStatus.NOT_STARTED.name());
    task.setPublishedTime(LocalDateTime.now());
    projectTaskService.updateById(task);

    Map<Long, SysUser> userMap =
        loadUsers(resolveUserIdsFromTasks(List.of(task), task.getCreatedBy()));
    return mapTaskResponse(task, userMap);
  }

  /** 更新任务状态 */
  @Transactional(rollbackFor = Exception.class)
  public TaskResponse updateTaskStatus(
      Long taskId, TaskStatus status, SysUser operator, boolean allowOverride) {
    ProjectTask task = projectTaskService.getById(taskId);
    if (task == null) {
      throw new BusinessException("任务不存在");
    }
    if (!allowOverride && !Objects.equals(task.getAssigneeId(), operator.getId())) {
      throw new BusinessException("只能更新分配给自己的任务");
    }
    if (status != TaskStatus.NOT_STARTED && status != TaskStatus.COMPLETED) {
      throw new BusinessException("不支持的任务状态");
    }
    task.setStatus(status.name());
    projectTaskService.updateById(task);

    Map<Long, SysUser> userMap =
        loadUsers(resolveUserIdsFromTasks(List.of(task), task.getCreatedBy()));
    return mapTaskResponse(task, userMap);
  }

  /** 获取可分配用户 */
  public List<TaskAssigneeResponse> listAssignableUsers() {
    List<SysUser> users = sysUserService.lambdaQuery().eq(SysUser::getRole, "USER").list();
    return users.stream()
        .sorted(Comparator.comparing(SysUser::getUsername))
        .map(
            user -> {
              TaskAssigneeResponse response = new TaskAssigneeResponse();
              response.setId(user.getId());
              response.setUsername(user.getUsername());
              return response;
            })
        .collect(Collectors.toList());
  }

  private Map<String, Integer> resolveColumnIndex(Row headerRow) {
    Map<String, Integer> indexMap = new HashMap<>(3);
    for (Cell cell : headerRow) {
      String value = getCellString(cell);
      if (!StringUtils.hasText(value)) {
        continue;
      }
      String header = value.trim();
      if (TITLE_HEADER.equals(header)) {
        indexMap.put(TITLE_HEADER, cell.getColumnIndex());
      } else if (DESCRIPTION_HEADER.equals(header)) {
        indexMap.put(DESCRIPTION_HEADER, cell.getColumnIndex());
      } else if (WORKLOAD_HEADER.equals(header)) {
        indexMap.put(WORKLOAD_HEADER, cell.getColumnIndex());
      }
    }
    if (!indexMap.keySet().containsAll(Set.of(TITLE_HEADER, DESCRIPTION_HEADER, WORKLOAD_HEADER))) {
      throw new BusinessException("Excel 表头必须包含：需求标题、需求描述、计入工作量(人天)");
    }
    return indexMap;
  }

  private String getCellString(Cell cell) {
    if (cell == null) {
      return "";
    }
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC ->
          BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
      case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
      case FORMULA -> cell.getCellFormula();
      default -> "";
    };
  }

  private BigDecimal parseWorkload(String raw, int rowIndex) {
    if (!StringUtils.hasText(raw)) {
      throw new BusinessException("第 " + (rowIndex + 1) + " 行工作量不能为空");
    }
    try {
      BigDecimal value = new BigDecimal(raw.trim());
      if (value.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException("第 " + (rowIndex + 1) + " 行工作量必须大于 0");
      }
      return value.setScale(2, RoundingMode.HALF_UP);
    } catch (NumberFormatException ex) {
      throw new BusinessException("第 " + (rowIndex + 1) + " 行工作量格式不合法");
    }
  }

  private Map<Long, SysUser> loadAssignableUsers(Set<Long> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Map.of();
    }
    List<SysUser> users = sysUserService.listByIds(ids);
    return users.stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
  }

  private Map<Long, SysUser> loadUsers(Set<Long> userIds) {
    if (CollectionUtils.isEmpty(userIds)) {
      return Map.of();
    }
    return sysUserService.listByIds(userIds).stream()
        .collect(Collectors.toMap(SysUser::getId, Function.identity()));
  }

  private Set<Long> resolveUserIdsFromBatches(
      List<ProjectTaskBatch> batches, Map<Long, List<ProjectTask>> taskMap) {
    Set<Long> ids = new HashSet<>();
    for (ProjectTaskBatch batch : batches) {
      ids.add(batch.getCreatedBy());
      List<ProjectTask> tasks = taskMap.get(batch.getId());
      if (!CollectionUtils.isEmpty(tasks)) {
        ids.addAll(tasks.stream().map(ProjectTask::getAssigneeId).collect(Collectors.toSet()));
      }
    }
    return ids;
  }

  private Set<Long> resolveUserIdsFromTasks(List<ProjectTask> tasks, Long creatorId) {
    Set<Long> ids = tasks.stream().map(ProjectTask::getAssigneeId).collect(Collectors.toSet());
    if (creatorId != null) {
      ids.add(creatorId);
    }
    return ids;
  }

  private Map<Long, List<ProjectTask>> loadTasksByBatch(List<Long> batchIds) {
    if (CollectionUtils.isEmpty(batchIds)) {
      return Map.of();
    }
    List<ProjectTask> tasks =
        projectTaskService.lambdaQuery().in(ProjectTask::getBatchId, batchIds).list();
    return tasks.stream().collect(Collectors.groupingBy(ProjectTask::getBatchId));
  }

  private TaskBatchSummaryResponse buildBatchSummary(
      ProjectTaskBatch batch, List<ProjectTask> tasks, Map<Long, SysUser> userMap) {
    TaskBatchSummaryResponse response = new TaskBatchSummaryResponse();
    response.setId(batch.getId());
    response.setTitle(batch.getTitle());
    response.setDescription(batch.getDescription());
    response.setPublishedTime(batch.getPublishedTime());
    response.setCreatedBy(batch.getCreatedBy());
    SysUser creator = userMap.get(batch.getCreatedBy());
    response.setCreatedByName(creator != null ? creator.getUsername() : null);
    response.setTotalTasks(tasks.size());
    response.setCompletedTasks(
        (int)
            tasks.stream()
                .filter(task -> TaskStatus.COMPLETED.name().equalsIgnoreCase(task.getStatus()))
                .count());
    response.setTotalWorkload(sumWorkload(tasks));
    return response;
  }

  private TaskBatchSummaryResponse buildUserBatchSummary(
      ProjectTaskBatch batch,
      List<ProjectTask> tasks,
      Long assigneeId,
      Map<Long, SysUser> userMap) {
    List<ProjectTask> myTasks =
        tasks.stream().filter(task -> Objects.equals(task.getAssigneeId(), assigneeId)).toList();
    TaskBatchSummaryResponse response = buildBatchSummary(batch, myTasks, userMap);
    response.setTotalTasks(myTasks.size());
    response.setCompletedTasks(
        (int)
            myTasks.stream()
                .filter(task -> TaskStatus.COMPLETED.name().equalsIgnoreCase(task.getStatus()))
                .count());
    response.setTotalWorkload(sumWorkload(myTasks));
    return response;
  }

  private TaskBatchDetailResponse buildBatchDetailResponse(
      ProjectTaskBatch batch, List<ProjectTask> tasks, Map<Long, SysUser> userMap) {
    TaskBatchDetailResponse response = new TaskBatchDetailResponse();
    response.setId(batch.getId());
    response.setTitle(batch.getTitle());
    response.setDescription(batch.getDescription());
    response.setPublishedTime(batch.getPublishedTime());
    response.setCreatedBy(batch.getCreatedBy());
    SysUser creator = userMap.get(batch.getCreatedBy());
    response.setCreatedByName(creator != null ? creator.getUsername() : null);
    response.setTotalTasks(tasks.size());
    response.setCompletedTasks(
        (int)
            tasks.stream()
                .filter(task -> TaskStatus.COMPLETED.name().equalsIgnoreCase(task.getStatus()))
                .count());
    response.setTotalWorkload(sumWorkload(tasks));
    response.setTasks(
        tasks.stream()
            .sorted(Comparator.comparing(ProjectTask::getId))
            .map(task -> mapTaskResponse(task, userMap))
            .collect(Collectors.toList()));
    return response;
  }

  private TaskResponse mapTaskResponse(ProjectTask task, Map<Long, SysUser> userMap) {
    TaskResponse response = new TaskResponse();
    response.setId(task.getId());
    response.setBatchId(task.getBatchId());
    response.setTitle(task.getTitle());
    response.setDescription(task.getDescription());
    response.setWorkloadManDay(task.getWorkloadManDay());
    String statusValue = task.getStatus();
    if (!StringUtils.hasText(statusValue)) {
      throw new BusinessException("任务状态异常");
    }
    TaskStatus status = TaskStatus.fromValue(statusValue);
    response.setStatus(status);
    response.setStatusLabel(status.getLabel());
    response.setAssigneeId(task.getAssigneeId());
    SysUser assignee = userMap.get(task.getAssigneeId());
    response.setAssigneeName(assignee != null ? assignee.getUsername() : null);
    response.setCreatedBy(task.getCreatedBy());
    SysUser creator = userMap.get(task.getCreatedBy());
    response.setCreatedByName(creator != null ? creator.getUsername() : null);
    response.setPublishedTime(task.getPublishedTime());
    response.setCreatedTime(task.getCreatedTime());
    response.setUpdatedTime(task.getUpdatedTime());
    return response;
  }

  private BigDecimal sumWorkload(List<ProjectTask> tasks) {
    BigDecimal total =
        tasks.stream()
            .map(ProjectTask::getWorkloadManDay)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return total.setScale(2, RoundingMode.HALF_UP);
  }
}
