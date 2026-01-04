package com.excalicode.platform.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.excalicode.platform.core.entity.ChatBiMessage;
import org.apache.ibatis.annotations.Mapper;

/** ChatBI 消息表 Mapper */
@Mapper
public interface ChatBiMessageMapper extends BaseMapper<ChatBiMessage> {}
