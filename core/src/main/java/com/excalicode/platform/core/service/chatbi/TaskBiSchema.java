package com.excalicode.platform.core.service.chatbi;

import java.util.Map;
import java.util.Set;

/** 任务域（TASK）允许的字段与表达式映射（用于 SQL 构建与校验）。 */
final class TaskBiSchema {

  private TaskBiSchema() {}

  static final String DATASET_CODE = "TASK";

  static final Set<String> ALLOWED_FIELDS =
      Set.of(
          "TASK_ID",
          "BATCH_ID",
          "BATCH_TITLE",
          "TITLE",
          "STATUS",
          "ASSIGNEE_ID",
          "ASSIGNEE_NAME",
          "CREATED_BY",
          "PUBLISHED_TIME",
          "WORKLOAD_MAN_DAY",
          "PUBLISHED_AGE_DAYS");

  static final Set<String> JOIN_REQUIRED_FIELDS = Set.of("ASSIGNEE_NAME", "BATCH_TITLE");

  static final Map<String, String> SELECT_EXPRESSION_BY_FIELD =
      Map.ofEntries(
          Map.entry("TASK_ID", "t.id"),
          Map.entry("BATCH_ID", "t.batch_id"),
          Map.entry("BATCH_TITLE", "b.title"),
          Map.entry("TITLE", "t.title"),
          Map.entry("STATUS", "t.status"),
          Map.entry("ASSIGNEE_ID", "t.assignee_id"),
          Map.entry("ASSIGNEE_NAME", "u.username"),
          Map.entry("CREATED_BY", "t.created_by"),
          Map.entry("PUBLISHED_TIME", "t.published_time"),
          Map.entry("WORKLOAD_MAN_DAY", "t.workload_man_day"),
          Map.entry("PUBLISHED_AGE_DAYS", "TIMESTAMPDIFF(DAY, t.published_time, NOW())"));

  static final Set<String> ALLOWED_AGGS =
      Set.of("COUNT", "COUNT_DISTINCT", "SUM", "AVG", "MIN", "MAX");

  static final Set<String> ALLOWED_FILTER_OPS =
      Set.of("EQ", "NE", "IN", "GT", "GTE", "LT", "LTE", "BETWEEN");

  static final Set<String> ALLOWED_ORDER_DIRECTIONS = Set.of("ASC", "DESC");

  static final Set<String> ALLOWED_TABLES =
      Set.of("project_task", "sys_user", "project_task_batch");

  static boolean requiresJoin(Set<String> usedFields) {
    for (String field : usedFields) {
      if (JOIN_REQUIRED_FIELDS.contains(field)) {
        return true;
      }
    }
    return false;
  }
}
