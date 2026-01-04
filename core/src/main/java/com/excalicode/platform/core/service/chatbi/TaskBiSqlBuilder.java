package com.excalicode.platform.core.service.chatbi;

import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.chatbi.ChatBiBuiltQuery;
import com.excalicode.platform.core.model.chatbi.ChatBiQueryPlan;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** 任务域 SQL 构建器：将已校验的查询计划转为参数化 SQL。 */
public final class TaskBiSqlBuilder {

  private TaskBiSqlBuilder() {}

  public static ChatBiBuiltQuery build(ChatBiPlanValidator.ValidatedPlan validatedPlan) {
    ChatBiQueryPlan plan = validatedPlan.plan();

    Set<String> usedFields = new HashSet<>(validatedPlan.usedFields());
    boolean needsJoin = TaskBiSchema.requiresJoin(usedFields);

    List<String> selectParts = new ArrayList<>();
    List<String> groupByParts = new ArrayList<>();
    List<String> columns = new ArrayList<>();

    int colIndex = 0;

    for (ChatBiQueryPlan.Dimension dim : safeList(plan.getDimensions())) {
      String field = dim.getField().trim();
      String displayName = normalizeAlias(dim.getAlias(), field);
      String expr = expr(field);

      String sqlAlias = "c" + colIndex;
      colIndex++;

      selectParts.add(expr + " AS " + sqlAlias);
      groupByParts.add(expr);
      columns.add(displayName);
    }

    for (ChatBiQueryPlan.Measure measure : safeList(plan.getMeasures())) {
      String agg = measure.getAgg().trim().toUpperCase(Locale.ROOT);
      String field = measure.getField().trim();
      String displayName = normalizeAlias(measure.getAlias(), agg + "_" + field);
      String aggExpr = aggExpression(agg, field);

      String sqlAlias = "c" + colIndex;
      colIndex++;

      selectParts.add(aggExpr + " AS " + sqlAlias);
      columns.add(displayName);
    }

    if (selectParts.isEmpty()) {
      throw new BusinessException("查询计划缺少可选字段");
    }

    List<String> whereParts = new ArrayList<>();
    List<Object> params = new ArrayList<>();

    whereParts.add("t.deleted = 0");

    for (ChatBiQueryPlan.Filter filter : safeList(plan.getFilters())) {
      appendFilter(filter, whereParts, params);
    }

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT ").append(String.join(", ", selectParts)).append(" FROM project_task t ");

    if (needsJoin) {
      sql.append("LEFT JOIN sys_user u ON u.id = t.assignee_id AND u.deleted = 0 ");
      sql.append("LEFT JOIN project_task_batch b ON b.id = t.batch_id AND b.deleted = 0 ");
    }

    if (!whereParts.isEmpty()) {
      sql.append("WHERE ").append(String.join(" AND ", whereParts)).append(" ");
    }

    if (!groupByParts.isEmpty()) {
      sql.append("GROUP BY ").append(String.join(", ", groupByParts)).append(" ");
    }

    List<String> orderByParts = new ArrayList<>();
    for (ChatBiQueryPlan.OrderBy order : safeList(plan.getOrderBy())) {
      String field = order.getField().trim();
      String direction = order.getDirection().trim().toUpperCase(Locale.ROOT);
      orderByParts.add(expr(field) + " " + direction);
    }
    if (!orderByParts.isEmpty()) {
      sql.append("ORDER BY ").append(String.join(", ", orderByParts)).append(" ");
    }

    sql.append("LIMIT ").append(validatedPlan.limit());

    return new ChatBiBuiltQuery(sql.toString(), params, columns);
  }

  private static void appendFilter(
      ChatBiQueryPlan.Filter filter, List<String> whereParts, List<Object> params) {
    if (filter == null
        || !StringUtils.hasText(filter.getField())
        || !StringUtils.hasText(filter.getOp())) {
      return;
    }

    String field = filter.getField().trim();
    String op = filter.getOp().trim().toUpperCase(Locale.ROOT);
    String expr = expr(field);

    switch (op) {
      case "EQ" -> {
        whereParts.add(expr + " = ?");
        params.add(castParam(field, filter.getValue()));
      }
      case "NE" -> {
        whereParts.add(expr + " <> ?");
        params.add(castParam(field, filter.getValue()));
      }
      case "GT" -> {
        whereParts.add(expr + " > ?");
        params.add(castParam(field, filter.getValue()));
      }
      case "GTE" -> {
        whereParts.add(expr + " >= ?");
        params.add(castParam(field, filter.getValue()));
      }
      case "LT" -> {
        whereParts.add(expr + " < ?");
        params.add(castParam(field, filter.getValue()));
      }
      case "LTE" -> {
        whereParts.add(expr + " <= ?");
        params.add(castParam(field, filter.getValue()));
      }
      case "IN" -> {
        if (CollectionUtils.isEmpty(filter.getValues())) {
          throw new BusinessException("IN 操作缺少 values");
        }
        List<String> values = filter.getValues();
        List<String> placeholders = new ArrayList<>();
        for (String raw : values) {
          if (!StringUtils.hasText(raw)) {
            continue;
          }
          placeholders.add("?");
          params.add(castParam(field, raw.trim()));
        }
        if (placeholders.isEmpty()) {
          throw new BusinessException("IN 操作缺少有效 values");
        }
        whereParts.add(expr + " IN (" + String.join(",", placeholders) + ")");
      }
      case "BETWEEN" -> {
        whereParts.add(expr + " BETWEEN ? AND ?");
        params.add(castParam(field, filter.getFrom()));
        params.add(castParam(field, filter.getTo()));
      }
      default -> throw new BusinessException("不支持的过滤操作符: " + op);
    }
  }

  private static Object castParam(String field, String rawValue) {
    if (!StringUtils.hasText(rawValue)) {
      return null;
    }

    String trimmed = rawValue.trim();
    if ("PUBLISHED_TIME".equals(field)) {
      LocalDateTime dt = ChatBiPlanValidator.parseDateTimeLoose(trimmed);
      return dt != null ? Timestamp.valueOf(dt) : null;
    }

    if ("PUBLISHED_AGE_DAYS".equals(field)) {
      try {
        return Integer.parseInt(trimmed);
      } catch (NumberFormatException ex) {
        throw new BusinessException("PUBLISHED_AGE_DAYS 必须是整数");
      }
    }

    if ("WORKLOAD_MAN_DAY".equals(field)) {
      try {
        return Double.parseDouble(trimmed);
      } catch (NumberFormatException ex) {
        throw new BusinessException("WORKLOAD_MAN_DAY 必须是数字");
      }
    }

    if ("ASSIGNEE_ID".equals(field)
        || "BATCH_ID".equals(field)
        || "TASK_ID".equals(field)
        || "CREATED_BY".equals(field)) {
      try {
        return Long.parseLong(trimmed);
      } catch (NumberFormatException ex) {
        throw new BusinessException(field + " 必须是数字");
      }
    }

    return trimmed;
  }

  private static String aggExpression(String agg, String field) {
    String expr = expr(field);
    return switch (agg) {
      case "COUNT" -> "COUNT(" + expr + ")";
      case "COUNT_DISTINCT" -> "COUNT(DISTINCT " + expr + ")";
      case "SUM" -> "SUM(" + expr + ")";
      case "AVG" -> "AVG(" + expr + ")";
      case "MIN" -> "MIN(" + expr + ")";
      case "MAX" -> "MAX(" + expr + ")";
      default -> throw new BusinessException("不支持的聚合: " + agg);
    };
  }

  private static String expr(String field) {
    String normalized = field == null ? "" : field.trim();
    String expr = TaskBiSchema.SELECT_EXPRESSION_BY_FIELD.get(normalized);
    if (!StringUtils.hasText(expr)) {
      throw new BusinessException("不支持的字段: " + field);
    }
    return expr;
  }

  private static String normalizeAlias(String alias, String fallback) {
    String value = StringUtils.hasText(alias) ? alias.trim() : fallback;
    // 防止奇怪字符
    return value.replaceAll("[^0-9A-Za-z_\u4e00-\u9fa5]", "_");
  }

  private static <T> List<T> safeList(List<T> list) {
    return list == null ? List.of() : list;
  }
}
