package com.excalicode.platform.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.excalicode.platform.core.entity.AiPromptTemplate;
import org.apache.ibatis.annotations.Mapper;

/** 提示词模板 Mapper 接口 */
@Mapper
public interface AiPromptTemplateMapper extends BaseMapper<AiPromptTemplate> {}
