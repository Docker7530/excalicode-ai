package com.excalicode.platform.core.service.entity.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.entity.ChatBiMessage;
import com.excalicode.platform.core.mapper.ChatBiMessageMapper;
import com.excalicode.platform.core.service.entity.ChatBiMessageService;
import org.springframework.stereotype.Service;

/** ChatBI 消息实体 Service 实现 */
@Service
public class ChatBiMessageServiceImpl extends ServiceImpl<ChatBiMessageMapper, ChatBiMessage>
    implements ChatBiMessageService {}
