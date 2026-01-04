package com.excalicode.platform.core.service.chatbi;

import com.excalicode.platform.core.entity.SysUser;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.chatbi.ChatBiQueryPlan;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.util.StringUtils;

/** ChatBI 权限问题。 */
public final class ChatBiPermissionGuard {

  private ChatBiPermissionGuard() {}

  public static GuardResult applyAndMaybeClarify(
      SysUser user, String question, ChatBiQueryPlan plan) {
    Objects.requireNonNull(user, "user");

    if (plan == null) {
      throw new BusinessException("查询计划为空");
    }

    if (isAdmin(user)) {
      return GuardResult.allow(plan);
    }

    Long userId = user.getId();
    if (userId == null) {
      throw new BusinessException("当前用户ID为空");
    }

    boolean hasExplicitAssigneeScope = hasAssigneeScopeFilter(plan, userId);

    if (!hasExplicitAssigneeScope && looksLikeCrossPersonQuestion(question, plan)) {
      ChatBiQueryPlan clarified = new ChatBiQueryPlan();
      clarified.setDataset(plan.getDataset());
      clarified.setNeedClarification(true);
      clarified.setClarifyingQuestion("你是普通用户，我只能查询你自己的任务。要改为查询“我的任务”相关统计吗？");
      return GuardResult.clarify(clarified);
    }

    // 默认我的任务范围
    enforceAssigneeScope(plan, userId);
    return GuardResult.allow(plan);
  }

  private static boolean isAdmin(SysUser user) {
    return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
  }

  private static boolean looksLikeCrossPersonQuestion(String question, ChatBiQueryPlan plan) {
    if (plan != null) {
      if (containsField(plan.getDimensions(), "ASSIGNEE_ID")
          || containsField(plan.getDimensions(), "ASSIGNEE_NAME")) {
        return true;
      }
      if (plan.getMeasures() != null) {
        for (ChatBiQueryPlan.Measure measure : plan.getMeasures()) {
          if (measure == null) {
            continue;
          }
          String agg = normalizeUpper(measure.getAgg());
          String field = normalize(measure.getField());
          if ("COUNT_DISTINCT".equals(agg) && "ASSIGNEE_ID".equals(field)) {
            return true;
          }
        }
      }
    }

    if (!StringUtils.hasText(question)) {
      return false;
    }

    String q = question.trim();
    return q.contains("多少人")
        || q.contains("每个人")
        || q.contains("所有人")
        || q.contains("全员")
        || q.contains("大家")
        || q.contains("人员");
  }

  private static boolean hasAssigneeScopeFilter(ChatBiQueryPlan plan, Long userId) {
    if (plan == null || plan.getFilters() == null) {
      return false;
    }

    for (ChatBiQueryPlan.Filter filter : plan.getFilters()) {
      if (filter == null) {
        continue;
      }
      if (!"ASSIGNEE_ID".equals(normalize(filter.getField()))) {
        continue;
      }
      if (!"EQ".equals(normalizeUpper(filter.getOp()))) {
        continue;
      }
      String value = normalize(filter.getValue());
      return value != null && value.equals(String.valueOf(userId));
    }

    return false;
  }

  private static void enforceAssigneeScope(ChatBiQueryPlan plan, Long userId) {
    if (plan.getFilters() == null) {
      plan.setFilters(new ArrayList<>());
    }

    // 清理想越权的 ASSIGNEE_ID 过滤，然后改到当前用户范围
    List<ChatBiQueryPlan.Filter> retained = new ArrayList<>();
    for (ChatBiQueryPlan.Filter filter : plan.getFilters()) {
      if (filter == null) {
        continue;
      }
      if ("ASSIGNEE_ID".equals(normalize(filter.getField()))) {
        continue;
      }
      retained.add(filter);
    }

    ChatBiQueryPlan.Filter scope = new ChatBiQueryPlan.Filter();
    scope.setField("ASSIGNEE_ID");
    scope.setOp("EQ");
    scope.setValue(String.valueOf(userId));
    retained.add(scope);

    plan.setFilters(retained);
  }

  private static boolean containsField(List<ChatBiQueryPlan.Dimension> dims, String field) {
    if (dims == null) {
      return false;
    }
    for (ChatBiQueryPlan.Dimension dim : dims) {
      if (dim != null && field.equals(normalize(dim.getField()))) {
        return true;
      }
    }
    return false;
  }

  private static String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private static String normalizeUpper(String value) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
  }

  public record GuardResult(boolean allowExecute, ChatBiQueryPlan plan) {
    static GuardResult allow(ChatBiQueryPlan plan) {
      return new GuardResult(true, plan);
    }

    static GuardResult clarify(ChatBiQueryPlan plan) {
      return new GuardResult(false, plan);
    }
  }
}
