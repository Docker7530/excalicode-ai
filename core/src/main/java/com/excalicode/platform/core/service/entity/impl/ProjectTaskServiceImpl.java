package com.excalicode.platform.core.service.entity.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.entity.ProjectTask;
import com.excalicode.platform.core.mapper.ProjectTaskMapper;
import com.excalicode.platform.core.service.entity.ProjectTaskService;
import org.springframework.stereotype.Service;

/** 任务实体 Service 实现 */
@Service
public class ProjectTaskServiceImpl extends ServiceImpl<ProjectTaskMapper, ProjectTask>
    implements ProjectTaskService {}
