package com.excalicode.platform.core.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 提示词模板实体
 *
 * 存储 Markdown 格式的提示词模板，用于替代原来的 resources/prompts/ 静态文件
 */
@Data
@TableName("ai_prompt_template")
public class AiPromptTemplate {

    /**
     * 主键ID, 数据库自动生成
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 提示词唯一标识代码（如 "REQUIREMENT_DOC_GENERATOR"） 用于代码中引用，保持人类可读性和配置可移植性
     */
    @TableField("code")
    private String code;

    /**
     * 提示词显示名称
     */
    @TableField("name")
    private String name;

    /**
     * Markdown 格式的提示词内容 使用 TEXT 类型存储，支持长文本
     */
    @TableField("content")
    private String content;

    /**
     * 提示词说明
     */
    @TableField("description")
    private String description;

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
}
