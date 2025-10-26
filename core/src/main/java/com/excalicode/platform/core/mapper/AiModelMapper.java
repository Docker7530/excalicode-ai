package com.excalicode.platform.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.excalicode.platform.core.entity.AiModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型 Mapper 接口
 * 继承 BaseMapper 即可获得基础 CRUD 能力
 */
@Mapper
public interface AiModelMapper
        extends BaseMapper<AiModel> {
}
