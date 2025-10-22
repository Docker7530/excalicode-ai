package com.excalicode.platform.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.excalicode.platform.core.entity.AiProvider;

/**
 * AI 厂商 Mapper 接口
 *
 * 继承 BaseMapper 即可获得基础 CRUD 能力
 */
@Mapper
public interface AiProviderMapper extends BaseMapper<AiProvider> {
}
