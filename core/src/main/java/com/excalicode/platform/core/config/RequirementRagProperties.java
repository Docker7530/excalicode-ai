package com.excalicode.platform.core.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** RAG 工厂配置 */
@Data
@Component
@ConfigurationProperties(prefix = "cosmic.requirement.rag")
public class RequirementRagProperties {

  /** 是否启用 RAG 功能 */
  private boolean enabled;

  /** 每次取回的文档数量 */
  private int topK;

  /** 文档相似度阈值 */
  private double minScore;

  /** 文档段落长度 */
  private int chunkSize;

  /** 段落重叠长度 */
  private int chunkOverlap;

  /** Redis 中 chunk id 的 key 前缀 */
  private String redisChunkKeyPrefix;

  /** chunk id 在 Redis 中的存活周期 */
  private Duration chunkKeyTtl;

  /** 是否开启重排 */
  private boolean rerankEnabled;

  /** 重排模型名称 */
  private String rerankModel;

  /** 重排接口路径 */
  private String rerankEndpoint;

  /** 可选：重排接口独立 BaseUrl */
  private String rerankBaseUrl;

  /** 重排 TopN 结果返回阈值 */
  private int rerankTopN;

  /** 重排 HTTP 调用超时 */
  private Duration rerankTimeout;
}
