package com.excalicode.platform.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 厂商实体
 * 存储 AI 模型厂商的基本信息, 包括厂商名称、API地址、密钥等
 */
@Data
@TableName("ai_provider")
public class AiProvider {

    /**
     * 主键ID, 数据库自动生成
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 厂商名称 (如: OpenAI, Anthropic, Google)
     */
    @TableField("provider_name")
    private String providerName;

    /**
     * API 基础地址 (如: <a href="https://api.openai.com/v1">...</a>)
     */
    @TableField("base_url")
    private String baseUrl;

    /**
     * API 密钥, 用于身份认证
     */
    @TableField("api_key")
    private String apiKey;

    /**
     * 掩码后的 API Key（用于前端展示, 不存数据库）
     */
    @TableField(exist = false)
    private String maskedApiKey;

    /**
     * 创建时间, 插入时自动填充
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间, 插入和更新时自动填充
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 逻辑删除标记: 0-未删除, 1-已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 关联的模型列表 (不存储在数据库, 用于关联查询)
     */
    @TableField(exist = false)
    private List<AiModel> models;
}
