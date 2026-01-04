package com.excalicode.platform.core.service.entity.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.entity.ChatBiSession;
import com.excalicode.platform.core.mapper.ChatBiSessionMapper;
import com.excalicode.platform.core.service.entity.ChatBiSessionService;
import org.springframework.stereotype.Service;

/** ChatBI 会话实体 Service 实现 */
@Service
public class ChatBiSessionServiceImpl extends ServiceImpl<ChatBiSessionMapper, ChatBiSession>
    implements ChatBiSessionService {}
