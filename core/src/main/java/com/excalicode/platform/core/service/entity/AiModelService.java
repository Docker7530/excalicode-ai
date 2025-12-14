package com.excalicode.platform.core.service.entity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.excalicode.platform.core.entity.AiModel;
import java.util.List;

/** AI 模型 Service 接口 */
public interface AiModelService extends IService<AiModel> {

  /**
   * 根据厂商ID查询该厂商的所有模型
   *
   * @param providerId 厂商ID
   * @return 模型列表
   */
  List<AiModel> listByProviderId(Long providerId);
}
