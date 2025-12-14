package com.excalicode.platform.core.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.entity.AiModel;
import com.excalicode.platform.core.mapper.AiModelMapper;
import com.excalicode.platform.core.service.entity.AiModelService;
import java.util.List;
import org.springframework.stereotype.Service;

/** AI 模型 Service 实现类 */
@Service
public class AiModelServiceImpl extends ServiceImpl<AiModelMapper, AiModel>
    implements AiModelService {

  @Override
  public List<AiModel> listByProviderId(Long providerId) {
    return this.list(
        new LambdaQueryWrapper<AiModel>()
            .eq(AiModel::getProviderId, providerId)
            .orderByDesc(AiModel::getCreatedTime));
  }
}
