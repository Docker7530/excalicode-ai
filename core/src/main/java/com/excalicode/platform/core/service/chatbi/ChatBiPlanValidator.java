package com.excalicode.platform.core.service.chatbi;

import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.chatbi.ChatBiQueryPlan;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** ChatBI 查询计划校验器 */
public final class ChatBiPlanValidator {

  private ChatBiPlanValidator() {}

  static final int DEFAULT_LIMIT = 50;
  static final int MAX_LIMIT = 200;

  public static ValidatedPlan validateAndNormalize(ChatBiQueryPlan plan) {
    if (plan == null) {
      throw new BusinessException("查询计划为空");
    }

    if (!TaskBiSchema.DATASET_CODE.equalsIgnoreCase(nullToEmpty(plan.getDataset()))) {
      throw new BusinessException("不支持的数据集: " + plan.getDataset());
    }

    boolean needClarification = Boolean.TRUE.equals(plan.getNeedClarification());
    if (needClarification) {
      if (!StringUtils.hasText(plan.getClarifyingQuestion())) {
        throw new BusinessException("needClarification=true 时必须给出 clarifyingQuestion");
      }
      return new ValidatedPlan(plan, Set.of(), DEFAULT_LIMIT);
    }

    if (CollectionUtils.isEmpty(plan.getMeasures())) {
      throw new BusinessException("查询计划缺少 measures");
    }

    int limit = normalizeLimit(plan.getLimit());
    plan.setLimit(limit);

    Set<String> usedFields = new HashSet<>();

    for (ChatBiQueryPlan.Dimension dim : safeList(plan.getDimensions())) {
      requireText(dim.getField(), "dimensions.field");
      validateField(dim.getField(), "dimensions.field");
      usedFields.add(dim.getField().trim());
      if (!StringUtils.hasText(dim.getAlias())) {
        dim.setAlias(dim.getField());
      }
    }

    for (ChatBiQueryPlan.Measure measure : safeList(plan.getMeasures())) {
      requireText(measure.getAgg(), "measures.agg");
      requireText(measure.getField(), "measures.field");
      validateAgg(measure.getAgg());
      validateField(measure.getField(), "measures.field");
      validateAggFieldCompatibility(measure.getAgg(), measure.getField());
      usedFields.add(measure.getField().trim());
      if (!StringUtils.hasText(measure.getAlias())) {
        measure.setAlias(measure.getAgg() + "_" + measure.getField());
      }
    }

    for (ChatBiQueryPlan.Filter filter : safeList(plan.getFilters())) {
      requireText(filter.getField(), "filters.field");
      requireText(filter.getOp(), "filters.op");
      validateField(filter.getField(), "filters.field");
      validateFilterOp(filter.getOp());
      usedFields.add(filter.getField().trim());
      normalizeFilterValue(filter);
    }

    for (ChatBiQueryPlan.OrderBy order : safeList(plan.getOrderBy())) {
      requireText(order.getField(), "orderBy.field");
      validateField(order.getField(), "orderBy.field");
      usedFields.add(order.getField().trim());
      String direction = nullToEmpty(order.getDirection()).trim().toUpperCase(Locale.ROOT);
      if (!TaskBiSchema.ALLOWED_ORDER_DIRECTIONS.contains(direction)) {
        throw new BusinessException("不支持的排序方向: " + order.getDirection());
      }
      order.setDirection(direction);
    }

    return new ValidatedPlan(plan, usedFields, limit);
  }

  private static void validateAgg(String aggRaw) {
    String agg = aggRaw.trim().toUpperCase(Locale.ROOT);
    if (!TaskBiSchema.ALLOWED_AGGS.contains(agg)) {
      throw new BusinessException("不支持的聚合: " + aggRaw);
    }
  }

  private static void validateFilterOp(String opRaw) {
    String op = opRaw.trim().toUpperCase(Locale.ROOT);
    if (!TaskBiSchema.ALLOWED_FILTER_OPS.contains(op)) {
      throw new BusinessException("不支持的过滤操作符: " + opRaw);
    }
  }

  private static void validateAggFieldCompatibility(String aggRaw, String fieldRaw) {
    String agg = aggRaw.trim().toUpperCase(Locale.ROOT);
    String field = fieldRaw.trim();

    if ("COUNT".equals(agg) || "COUNT_DISTINCT".equals(agg)) {
      return;
    }

    if (!"WORKLOAD_MAN_DAY".equals(field) && !"PUBLISHED_AGE_DAYS".equals(field)) {
      throw new BusinessException("聚合 " + agg + " 仅允许作用于 WORKLOAD_MAN_DAY 或 PUBLISHED_AGE_DAYS");
    }
  }

  private static void validateField(String fieldRaw, String path) {
    String field = fieldRaw.trim();
    if (!TaskBiSchema.ALLOWED_FIELDS.contains(field)) {
      throw new BusinessException("不支持的字段(" + path + "): " + fieldRaw);
    }
  }

  private static void normalizeFilterValue(ChatBiQueryPlan.Filter filter) {
    String op = filter.getOp().trim().toUpperCase(Locale.ROOT);
    filter.setOp(op);

    if ("IN".equals(op)) {
      if (CollectionUtils.isEmpty(filter.getValues())) {
        throw new BusinessException("IN 操作必须提供 values");
      }
      List<String> normalized =
          filter.getValues().stream().filter(StringUtils::hasText).map(String::trim).toList();
      if (normalized.isEmpty()) {
        throw new BusinessException("IN 操作必须提供非空 values");
      }
      filter.setValues(normalized);
      return;
    }

    if ("BETWEEN".equals(op)) {
      if (!StringUtils.hasText(filter.getFrom()) || !StringUtils.hasText(filter.getTo())) {
        throw new BusinessException("BETWEEN 操作必须提供 from/to");
      }
      filter.setFrom(filter.getFrom().trim());
      filter.setTo(filter.getTo().trim());
      return;
    }

    if (!StringUtils.hasText(filter.getValue())) {
      throw new BusinessException(op + " 操作必须提供 value");
    }
    filter.setValue(filter.getValue().trim());
  }

  static LocalDateTime parseDateTimeLoose(String input) {
    if (!StringUtils.hasText(input)) {
      return null;
    }

    String trimmed = input.trim();

    try {
      if (trimmed.length() == 10) {
        LocalDate date = LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return LocalDateTime.of(date, LocalTime.MIN);
      }

      if (trimmed.length() == 16) {
        return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
      }

      return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    } catch (DateTimeParseException ex) {
      throw new BusinessException("无法解析时间: " + input);
    }
  }

  private static int normalizeLimit(Integer raw) {
    int value = raw != null ? raw : DEFAULT_LIMIT;
    if (value <= 0) {
      value = DEFAULT_LIMIT;
    }
    if (value > MAX_LIMIT) {
      value = MAX_LIMIT;
    }
    return value;
  }

  private static void requireText(String value, String path) {
    if (!StringUtils.hasText(value)) {
      throw new BusinessException(path + " 不能为空");
    }
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static <T> List<T> safeList(List<T> list) {
    return list == null ? List.of() : list;
  }

  public record ValidatedPlan(ChatBiQueryPlan plan, Set<String> usedFields, int limit) {

    public ValidatedPlan {
      Objects.requireNonNull(plan, "plan");
      Objects.requireNonNull(usedFields, "usedFields");
    }

    List<String> dimensionFields() {
      List<String> fields = new ArrayList<>();
      for (ChatBiQueryPlan.Dimension dim : safeList(plan.getDimensions())) {
        if (dim != null && StringUtils.hasText(dim.getField())) {
          fields.add(dim.getField().trim());
        }
      }
      return fields;
    }
  }
}
