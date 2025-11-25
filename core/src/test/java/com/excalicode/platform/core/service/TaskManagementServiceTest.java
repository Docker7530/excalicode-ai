package com.excalicode.platform.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.excalicode.platform.common.exception.BusinessException;
import com.excalicode.platform.core.api.task.TaskBatchDetailResponse;
import com.excalicode.platform.core.api.task.TaskDraftResponse;
import com.excalicode.platform.core.api.task.TaskPublishRequest;
import com.excalicode.platform.core.api.task.TaskResponse;
import com.excalicode.platform.core.entity.ProjectTaskBatch;
import com.excalicode.platform.core.entity.SysUser;
import com.excalicode.platform.core.service.entity.ProjectTaskBatchService;
import com.excalicode.platform.core.service.entity.ProjectTaskService;
import com.excalicode.platform.core.service.entity.SysUserService;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class TaskManagementServiceTest {

  @Mock private ProjectTaskBatchService projectTaskBatchService;

  @Mock private ProjectTaskService projectTaskService;

  @Mock private SysUserService sysUserService;

  @InjectMocks private TaskManagementService taskManagementService;

  private SysUser creator;
  private SysUser assignee;

  @BeforeEach
  void setUp() {
    creator = new SysUser();
    creator.setId(1L);
    creator.setUsername("admin");
    creator.setRole("ADMIN");

    assignee = new SysUser();
    assignee.setId(2L);
    assignee.setUsername("user-a");
    assignee.setRole("USER");
  }

  @Test
  @DisplayName("Excel 模板解析成功返回任务草稿")
  void parseTaskTemplate_success() throws Exception {
    MockMultipartFile file = createExcelFile();

    List<TaskDraftResponse> drafts = taskManagementService.parseTaskTemplate(file);

    assertThat(drafts).hasSize(1);
    TaskDraftResponse draft = drafts.get(0);
    assertThat(draft.getTitle()).isEqualTo("任务一");
    assertThat(draft.getDescription()).isEqualTo("描述一");
    assertThat(draft.getWorkloadManDay()).isEqualByComparingTo("2.50");
  }

  @Test
  @DisplayName("发布任务时需要合法执行人")
  void publishTasks_success() {
    TaskPublishRequest.TaskPublishItem item = new TaskPublishRequest.TaskPublishItem();
    item.setRowIndex(2);
    item.setTitle("任务一");
    item.setDescription("描述一");
    item.setWorkloadManDay(new BigDecimal("1.0"));
    item.setAssigneeId(assignee.getId());

    TaskPublishRequest request = new TaskPublishRequest();
    request.setBatchTitle("批次一");
    request.setTasks(List.of(item));

    when(projectTaskBatchService.save(any(ProjectTaskBatch.class)))
        .thenAnswer(
            invocation -> {
              ProjectTaskBatch batch = invocation.getArgument(0);
              batch.setId(99L);
              return true;
            });
    when(sysUserService.listByIds(anySet())).thenReturn(List.of(assignee));
    when(projectTaskService.saveBatch(anyCollection())).thenReturn(true);

    TaskBatchDetailResponse detail = taskManagementService.publishTaskBatch(request, creator);

    assertThat(detail.getTasks()).hasSize(1);
    TaskResponse response = detail.getTasks().get(0);
    assertThat(response.getAssigneeId()).isEqualTo(assignee.getId());
    assertThat(response.getStatus()).isNotNull();
    verify(projectTaskBatchService).save(any(ProjectTaskBatch.class));
    verify(projectTaskService).saveBatch(anyCollection());
  }

  @Test
  @DisplayName("执行人角色非法时抛出异常")
  void publishTasks_invalidAssigneeRole() {
    assignee.setRole("ADMIN");

    TaskPublishRequest.TaskPublishItem item = new TaskPublishRequest.TaskPublishItem();
    item.setRowIndex(2);
    item.setTitle("任务一");
    item.setDescription("描述一");
    item.setWorkloadManDay(new BigDecimal("1.0"));
    item.setAssigneeId(assignee.getId());

    TaskPublishRequest request = new TaskPublishRequest();
    request.setBatchTitle("批次一");
    request.setTasks(List.of(item));

    when(sysUserService.listByIds(anySet())).thenReturn(List.of(assignee));

    assertThatThrownBy(() -> taskManagementService.publishTaskBatch(request, creator))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("执行人必须为 USER");
    verify(projectTaskBatchService, never()).save(any(ProjectTaskBatch.class));
    verify(projectTaskService, never()).saveBatch(anyCollection());
  }

  private MockMultipartFile createExcelFile() throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Sheet1");
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("需求标题");
      header.createCell(1).setCellValue("需求描述");
      header.createCell(2).setCellValue("计入工作量(人天)");

      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue("任务一");
      row.createCell(1).setCellValue("描述一");
      row.createCell(2).setCellValue("2.5");

      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        workbook.write(baos);
        return new MockMultipartFile(
            "file",
            "tasks.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            baos.toByteArray());
      }
    }
  }
}
