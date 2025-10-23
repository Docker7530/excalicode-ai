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
 * 功能-提示词映射实体
 *
 * 存储功能与提示词模板的绑定关系，实现功能级别的提示词切换
 */
@Data
@TableName("function_prompt_mapping")
public class FunctionPromptMapping {

    /**
     * 主键ID, 数据库自动生成
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 功能标识（对应 AiFunctionType 枚举的 code）
     */
    @TableField("function_code")
    private String functionCode;

    /**
     * 提示词代码（对应 prompt_template 表的 code 字段）
     */
    @TableField("prompt_code")
    private String promptCode;

    /**
     * 优先级（数字越大优先级越高） 用于一个功能绑定多个提示词时的排序
     */
    @TableField("priority")
    private Integer priority;

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
     * 关联的提示词模板信息 (不存储在数据库, 用于关联查询)
     */
    @TableField(exist = false)
    private PromptTemplate promptTemplate;
}
