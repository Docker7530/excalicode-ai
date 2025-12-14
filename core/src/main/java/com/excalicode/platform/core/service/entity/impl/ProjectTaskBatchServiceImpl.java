package com.excalicode.platform.core.service.entity.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.entity.ProjectTaskBatch;
import com.excalicode.platform.core.mapper.ProjectTaskBatchMapper;
import com.excalicode.platform.core.service.entity.ProjectTaskBatchService;
import org.springframework.stereotype.Service;

/** 任务批次 Service 实现 */
@Service
public class ProjectTaskBatchServiceImpl
    extends ServiceImpl<ProjectTaskBatchMapper, ProjectTaskBatch>
    implements ProjectTaskBatchService {}
