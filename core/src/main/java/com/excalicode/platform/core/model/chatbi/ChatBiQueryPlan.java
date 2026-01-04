package com.excalicode.platform.core.model.chatbi;

import java.util.List;
import lombok.Data;

/**
 * ChatBI 查询计划（由大模型生成，后端校验后再执行）。
 *
 * <p>当前仅支持任务域：TASK。
 */
@Data
public class ChatBiQueryPlan {

  /** 数据集代码：当前仅允许 TASK */
  private String dataset;

  /** 是否需要澄清（需要澄清时后端不会执行查询） */
  private Boolean needClarification;

  /** 澄清问题 */
  private String clarifyingQuestion;

  /** 指标（聚合项） */
  private List<Measure> measures;

  /** 维度（分组项） */
  private List<Dimension> dimensions;

  /** 过滤条件 */
  private List<Filter> filters;

  /** 排序 */
  private List<OrderBy> orderBy;

  /** 最大返回行数 */
  private Integer limit;

  @Data
  public static class Measure {
    /** 聚合：COUNT, COUNT_DISTINCT, SUM, AVG, MIN, MAX */
    private String agg;

    /** 字段语义码（如 TASK_ID / ASSIGNEE_ID / WORKLOAD_MAN_DAY） */
    private String field;

    /** 展示别名 */
    private String alias;
  }

  @Data
  public static class Dimension {
    /** 字段语义码 */
    private String field;

    /** 展示别名 */
    private String alias;
  }

  @Data
  public static class Filter {
    /** 字段语义码 */
    private String field;

    /** 操作符：EQ, NE, IN, GT, GTE, LT, LTE, BETWEEN */
    private String op;

    /** 单值过滤 */
    private String value;

    /** 多值过滤（IN） */
    private List<String> values;

    /** 范围过滤（BETWEEN）- 起始 */
    private String from;

    /** 范围过滤（BETWEEN）- 结束 */
    private String to;
  }

  @Data
  public static class OrderBy {
    /** 字段语义码 */
    private String field;

    /** ASC / DESC */
    private String direction;
  }
}
