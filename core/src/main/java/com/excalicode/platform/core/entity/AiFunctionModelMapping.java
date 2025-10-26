package com.excalicode.platform.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 功能-模型映射实体
 * 存储功能类型与 AI 模型的绑定关系，实现功能级别的模型切换
 */
@Data
@TableName("ai_function_model_mapping")
public class AiFunctionModelMapping {

    /**
     * 主键ID, 数据库自动生成
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 功能类型代码（对应 AiFunctionType 枚举的 code）
     */
    @TableField("function_type")
    private String functionType;

    /**
     * 模型ID, 外键关联 ai_model 表
     */
    @TableField("model_id")
    private Long modelId;

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
     * 关联的模型信息 (不存储在数据库, 用于关联查询)
     */
    @TableField(exist = false)
    private AiModel model;
}
