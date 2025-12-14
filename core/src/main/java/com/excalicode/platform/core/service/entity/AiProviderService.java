package com.excalicode.platform.core.service.entity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.excalicode.platform.core.entity.AiProvider;
import java.util.List;

/** AI 厂商 Service 接口 */
public interface AiProviderService extends IService<AiProvider> {

  /**
   * 查询所有厂商及其关联的所有模型
   *
   * @return 所有厂商及模型列表
   */
  List<AiProvider> listProvidersWithModels();

  /**
   * 删除厂商并一并逻辑删除其关联模型
   *
   * @param providerId 厂商ID
   */
  void removeProviderWithModels(Long providerId);
}
