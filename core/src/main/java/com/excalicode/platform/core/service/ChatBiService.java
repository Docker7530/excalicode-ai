package com.excalicode.platform.core.service;

import com.excalicode.platform.core.ai.AiFunctionExecutor;
import com.excalicode.platform.core.api.chatbi.ChatBiAskRequest;
import com.excalicode.platform.core.api.chatbi.ChatBiAskResponse;
import com.excalicode.platform.core.api.chatbi.ChatBiMessageResponse;
import com.excalicode.platform.core.api.chatbi.ChatBiQueryResultResponse;
import com.excalicode.platform.core.api.chatbi.ChatBiSessionDetailResponse;
import com.excalicode.platform.core.api.chatbi.ChatBiSessionSummaryResponse;
import com.excalicode.platform.core.entity.ChatBiMessage;
import com.excalicode.platform.core.entity.ChatBiSession;
import com.excalicode.platform.core.entity.SysUser;
import com.excalicode.platform.core.enums.AiFunctionType;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.chatbi.ChatBiBuiltQuery;
import com.excalicode.platform.core.model.chatbi.ChatBiQueryPlan;
import com.excalicode.platform.core.model.chatbi.ChatBiQueryResult;
import com.excalicode.platform.core.service.chatbi.ChatBiPermissionGuard;
import com.excalicode.platform.core.service.chatbi.ChatBiPlanValidator;
import com.excalicode.platform.core.service.chatbi.ChatBiSqlSafetyGuard;
import com.excalicode.platform.core.service.chatbi.TaskBiSqlBuilder;
import com.excalicode.platform.core.service.entity.ChatBiMessageService;
import com.excalicode.platform.core.service.entity.ChatBiSessionService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** ChatBI 核心服务（任务域）。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBiService {

  private static final int DEFAULT_SESSION_LIST_LIMIT = 20;

  private final AiFunctionExecutor aiFunctionExecutor;
  private final ChatBiSessionService chatBiSessionService;
  private final ChatBiMessageService chatBiMessageService;
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  /** 提问并返回 ChatBI 结果。 */
  @Transactional
  public ChatBiAskResponse ask(ChatBiAskRequest request, SysUser user) {
    Objects.requireNonNull(request, "request");
    SysUser currentUser = Objects.requireNonNull(user, "user");

    String question = normalizeQuestion(request.getQuestion());

    ChatBiSession session = ensureSessionAccessible(request.getSessionId(), currentUser, question);
    persistMessage(session.getId(), "USER", question, null, null, null, null);

    ChatBiAskResponse response = new ChatBiAskResponse();
    response.setSessionId(session.getId());

    String planJson = null;
    String executedSql = null;

    try {
      log.info(
          "ChatBI 提问: username={}, role={}, sessionId={}, question={}",
          currentUser.getUsername(),
          currentUser.getRole(),
          session.getId(),
          question);

      String roleHint = "ADMIN".equalsIgnoreCase(currentUser.getRole()) ? "ADMIN" : "USER";
      String roleInstruction =
          "当前登录用户信息：username="
              + currentUser.getUsername()
              + ", role="
              + roleHint
              + "。请严格按权限规则生成计划。";

      ChatBiQueryPlan plan =
          aiFunctionExecutor.executeStructured(
              AiFunctionType.CHAT_BI,
              List.of(new SystemMessage(roleInstruction), new UserMessage(question)),
              ChatBiQueryPlan.class);

      ChatBiPermissionGuard.GuardResult guard =
          ChatBiPermissionGuard.applyAndMaybeClarify(currentUser, question, plan);

      ChatBiQueryPlan guardedPlan = guard.plan();
      planJson = safeToJson(guardedPlan);

      if (!guard.allowExecute() || Boolean.TRUE.equals(guardedPlan.getNeedClarification())) {
        String clarifying =
            StringUtils.hasText(guardedPlan.getClarifyingQuestion())
                ? guardedPlan.getClarifyingQuestion()
                : "需要进一步澄清你的问题";

        response.setNeedClarification(true);
        response.setClarifyingQuestion(clarifying);
        response.setAnswer(clarifying);
        attachDebugIfAdmin(response, currentUser, null, planJson);

        persistMessage(session.getId(), "ASSISTANT", clarifying, planJson, null, null, null);
        touchSession(session);
        return response;
      }

      ChatBiPlanValidator.ValidatedPlan validated =
          ChatBiPlanValidator.validateAndNormalize(guardedPlan);
      ChatBiBuiltQuery built = TaskBiSqlBuilder.build(validated);
      executedSql = built.sql();
      ChatBiSqlSafetyGuard.assertSafeSelect(executedSql);

      ChatBiQueryResult result = executeQuery(built);
      ChatBiQueryResultResponse resultResponse = toResultResponse(result);

      String answer = buildAnswerText(result);

      response.setNeedClarification(false);
      response.setAnswer(answer);
      response.setResult(resultResponse);
      attachDebugIfAdmin(response, currentUser, executedSql, planJson);

      persistMessage(
          session.getId(),
          "ASSISTANT",
          answer,
          planJson,
          executedSql,
          safeToJson(resultResponse),
          null);
      touchSession(session);
      return response;
    } catch (BusinessException ex) {
      if (StringUtils.hasText(executedSql)) {
        log.warn("ChatBI 执行失败: {}, sql={}", ex.getMessage(), executedSql);
      } else {
        log.warn("ChatBI 执行失败: {}", ex.getMessage());
      }
      response.setNeedClarification(false);
      response.setAnswer(ex.getMessage());
      attachDebugIfAdmin(response, currentUser, executedSql, planJson);

      persistMessage(
          session.getId(),
          "ASSISTANT",
          ex.getMessage(),
          planJson,
          executedSql,
          null,
          ex.getMessage());
      touchSession(session);
      return response;
    } catch (Exception ex) {
      log.error("ChatBI 执行异常", ex);
      response.setNeedClarification(false);
      response.setAnswer("系统繁忙，请稍后重试");

      persistMessage(session.getId(), "ASSISTANT", "系统繁忙，请稍后重试", null, null, null, ex.getMessage());
      touchSession(session);
      return response;
    }
  }

  /** 获取当前用户最近会话列表。 */
  public List<ChatBiSessionSummaryResponse> listMySessions(SysUser user) {
    SysUser currentUser = Objects.requireNonNull(user, "user");

    List<ChatBiSession> sessions =
        chatBiSessionService
            .lambdaQuery()
            .eq(ChatBiSession::getUserId, currentUser.getId())
            .orderByDesc(ChatBiSession::getLastActiveTime)
            .last("LIMIT " + DEFAULT_SESSION_LIST_LIMIT)
            .list();

    if (CollectionUtils.isEmpty(sessions)) {
      return List.of();
    }

    return sessions.stream().map(this::toSummary).toList();
  }

  /** 获取会话详情（含消息）。 */
  public ChatBiSessionDetailResponse getSessionDetail(Long sessionId, SysUser user) {
    SysUser currentUser = Objects.requireNonNull(user, "user");
    ChatBiSession session =
        Objects.requireNonNull(chatBiSessionService.getById(sessionId), "会话不存在");

    assertSessionReadable(session, currentUser);

    List<ChatBiMessage> messages =
        chatBiMessageService
            .lambdaQuery()
            .eq(ChatBiMessage::getSessionId, sessionId)
            .orderByAsc(ChatBiMessage::getId)
            .list();

    ChatBiSessionDetailResponse response = new ChatBiSessionDetailResponse();
    response.setId(session.getId());
    response.setTitle(session.getTitle());
    response.setLastActiveTime(session.getLastActiveTime());
    response.setMessages(messages.stream().map(this::toMessageResponse).toList());
    return response;
  }

  private ChatBiSessionSummaryResponse toSummary(ChatBiSession session) {
    ChatBiSessionSummaryResponse summary = new ChatBiSessionSummaryResponse();
    summary.setId(session.getId());
    summary.setTitle(session.getTitle());
    summary.setLastActiveTime(session.getLastActiveTime());
    return summary;
  }

  private ChatBiMessageResponse toMessageResponse(ChatBiMessage message) {
    ChatBiMessageResponse response = new ChatBiMessageResponse();
    response.setId(message.getId());
    response.setRole(message.getRole());
    response.setContent(message.getContent());
    response.setCreatedTime(message.getCreatedTime());

    if (StringUtils.hasText(message.getResultJson())) {
      try {
        ChatBiQueryResultResponse result =
            objectMapper.readValue(message.getResultJson(), new TypeReference<>() {});
        response.setResult(result);
      } catch (Exception ex) {
        response.setResult(null);
      }
    }

    return response;
  }

  private ChatBiQueryResult executeQuery(ChatBiBuiltQuery built) {
    List<Object> params = built.params() != null ? built.params() : List.of();

    return jdbcTemplate.query(
        built.sql(),
        params.toArray(),
        (ResultSet rs) -> {
          List<List<Object>> rows = new ArrayList<>();
          ResultSetMetaData meta = rs.getMetaData();
          int colCount = meta.getColumnCount();
          while (rs.next()) {
            List<Object> row = new ArrayList<>(colCount);
            for (int i = 1; i <= colCount; i++) {
              row.add(rs.getObject(i));
            }
            rows.add(row);
          }
          return new ChatBiQueryResult(built.columns(), rows);
        });
  }

  private ChatBiQueryResultResponse toResultResponse(ChatBiQueryResult result) {
    ChatBiQueryResultResponse response = new ChatBiQueryResultResponse();
    response.setColumns(result.columns());
    response.setRows(result.rows());
    return response;
  }

  private String buildAnswerText(ChatBiQueryResult result) {
    if (result == null || CollectionUtils.isEmpty(result.rows())) {
      return "没有符合条件的数据";
    }

    List<String> columns = result.columns() != null ? result.columns() : List.of();

    if (result.rows().size() == 1) {
      List<Object> row = result.rows().getFirst();
      if (row == null || row.isEmpty()) {
        return "没有符合条件的数据";
      }
      StringBuilder sb = new StringBuilder();
      sb.append("结果：");
      for (int i = 0; i < row.size(); i++) {
        String col = i < columns.size() ? columns.get(i) : "col" + i;
        sb.append(col).append("=").append(String.valueOf(row.get(i)));
        if (i != row.size() - 1) {
          sb.append("，");
        }
      }
      return sb.toString();
    }

    return "已返回 " + result.rows().size() + " 行结果";
  }

  private ChatBiSession ensureSessionAccessible(Long sessionId, SysUser user, String question) {
    if (sessionId == null) {
      ChatBiSession created = new ChatBiSession();
      created.setUserId(user.getId());
      created.setTitle(buildSessionTitle(question));
      created.setLastActiveTime(LocalDateTime.now());
      chatBiSessionService.save(created);
      return created;
    }

    ChatBiSession existing = chatBiSessionService.getById(sessionId);
    if (existing == null) {
      throw new BusinessException("会话不存在");
    }
    assertSessionReadable(existing, user);
    return existing;
  }

  private void touchSession(ChatBiSession session) {
    if (session == null || session.getId() == null) {
      return;
    }
    ChatBiSession update = new ChatBiSession();
    update.setId(session.getId());
    update.setLastActiveTime(LocalDateTime.now());
    chatBiSessionService.updateById(update);
  }

  private void assertSessionReadable(ChatBiSession session, SysUser user) {
    if (session == null) {
      throw new BusinessException("会话不存在");
    }
    if (user == null) {
      throw new BusinessException("用户为空");
    }

    boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
    if (isAdmin) {
      return;
    }

    if (!Objects.equals(session.getUserId(), user.getId())) {
      throw new BusinessException("无权访问该会话");
    }
  }

  private void persistMessage(
      Long sessionId,
      String role,
      String content,
      String planJson,
      String executedSql,
      String resultJson,
      String errorMessage) {
    ChatBiMessage message = new ChatBiMessage();
    message.setSessionId(sessionId);
    message.setRole(role);
    message.setContent(content);
    message.setPlanJson(planJson);
    message.setExecutedSql(executedSql);
    message.setResultJson(resultJson);
    message.setErrorMessage(errorMessage);
    chatBiMessageService.save(message);
  }

  private void attachDebugIfAdmin(
      ChatBiAskResponse response, SysUser user, String sql, String planJson) {
    if (response == null || user == null) {
      return;
    }
    if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
      response.setDebugSql(null);
      response.setDebugPlan(null);
      return;
    }
    response.setDebugSql(sql);
    response.setDebugPlan(planJson);
  }

  private String safeToJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      return null;
    }
  }

  private String normalizeQuestion(String raw) {
    if (!StringUtils.hasText(raw)) {
      throw new BusinessException("问题不能为空");
    }
    return raw.trim();
  }

  private String buildSessionTitle(String question) {
    String q = question == null ? "" : question.trim();
    if (!StringUtils.hasText(q)) {
      return "ChatBI";
    }
    if (q.length() <= 40) {
      return q;
    }
    return q.substring(0, 40) + "…";
  }
}
