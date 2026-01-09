package com.excalicode.platform.core.service;

import com.excalicode.platform.core.config.RequirementRagProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** 负责调用外部重排模型，对向量检索结果进行语义重排序。 */
@Slf4j
@Service
public class DocumentRerankService {

  private final RequirementRagProperties properties;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public DocumentRerankService(
      RequirementRagProperties properties,
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${spring.ai.openai.base-url}") String openAiBaseUrl,
      @Value("${spring.ai.openai.api-key}") String openAiApiKey) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.restClient = buildRestClient(properties, restClientBuilder, openAiBaseUrl, openAiApiKey);
  }

  /**
   * 根据查询与候选内容调用重排模型。失败时返回空列表，不影响原始排序。
   *
   * @param query 用户查询
   * @param contents 候选文档内容
   * @return 重排结果（索引 + 分数）
   */
  public List<DocumentRerankResult> rerank(String query, List<String> contents) {
    if (!properties.isRerankEnabled()) {
      return List.of();
    }
    if (restClient == null) {
      log.warn("重排功能未初始化：缺少 RestClient 或鉴权信息");
      return List.of();
    }
    if (!StringUtils.hasText(query) || CollectionUtils.isEmpty(contents)) {
      return List.of();
    }
    int topN = Math.clamp(properties.getRerankTopN(), 1, contents.size());
    Map<String, Object> payload = new HashMap<>();
    payload.put("model", properties.getRerankModel());
    payload.put("query", query);
    payload.put("documents", contents);
    payload.put("top_n", topN);
    payload.put("return_documents", false);

    try {
      String raw =
          restClient
              .post()
              .uri(properties.getRerankEndpoint())
              .contentType(MediaType.APPLICATION_JSON)
              .body(payload)
              .retrieve()
              .body(String.class);
      return parseResponse(raw, contents.size(), topN);
    } catch (Exception ex) {
      log.warn("调用重排模型失败: {}", ex.getMessage());
      return List.of();
    }
  }

  private RestClient buildRestClient(
      RequirementRagProperties properties,
      RestClient.Builder builder,
      String openAiBaseUrl,
      String openAiApiKey) {
    if (!properties.isRerankEnabled()) {
      return null;
    }
    String baseUrl =
        StringUtils.hasText(properties.getRerankBaseUrl())
            ? properties.getRerankBaseUrl()
            : openAiBaseUrl;
    if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(openAiApiKey)) {
      log.warn("重排初始化失败：未配置 baseUrl 或 API Key");
      return null;
    }
    Duration timeout = properties.getRerankTimeout();
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    int millis = Math.toIntExact(timeout.toMillis());
    factory.setConnectTimeout(millis);
    factory.setReadTimeout(millis);
    return builder
        .baseUrl(baseUrl)
        .requestFactory(factory)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
        .build();
  }

  private List<DocumentRerankResult> parseResponse(String raw, int size, int topN) {
    if (!StringUtils.hasText(raw)) {
      return List.of();
    }
    try {
      JsonNode root = objectMapper.readTree(raw);
      JsonNode resultsNode = root.path("results");
      if (!resultsNode.isArray()) {
        resultsNode = root.path("data");
      }
      if (!resultsNode.isArray()) {
        return List.of();
      }
      List<DocumentRerankResult> results = new ArrayList<>();
      for (JsonNode node : resultsNode) {
        int index = node.path("index").asInt(-1);
        if (index < 0 && node.has("document_index")) {
          index = node.path("document_index").asInt(-1);
        }
        double score = node.path("relevance_score").asDouble(node.path("score").asDouble(0.0d));
        if (index >= 0 && index < size) {
          results.add(new DocumentRerankResult(index, score));
        }
      }
      results.sort(Comparator.comparingDouble(DocumentRerankResult::score).reversed());
      if (results.size() > topN) {
        return List.copyOf(results.subList(0, topN));
      }
      return List.copyOf(results);
    } catch (Exception ex) {
      log.warn("重排响应解析失败: {}", ex.getMessage());
      return List.of();
    }
  }

  /** 重排结果载体，保留原始索引与重排得分。 */
  public record DocumentRerankResult(int index, double score) {}
}
