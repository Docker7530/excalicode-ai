package com.excalicode.platform.core.service.entity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.excalicode.platform.core.entity.AiFunctionModelMapping;
import com.excalicode.platform.core.enums.AiFunctionType;
import java.util.List;

/** AI 功能-模型映射 Service 接口 */
public interface AiFunctionModelMappingService extends IService<AiFunctionModelMapping> {

  /**
   * 根据功能类型查询模型ID
   *
   * @param functionType 功能类型
   * @return 模型ID, 如果没有配置则返回 null
   */
  Long getModelIdByFunctionType(AiFunctionType functionType);

  /**
   * 设置功能类型的模型映射
   *
   * @param functionType 功能类型
   * @param modelId 模型ID
   * @return 是否设置成功
   */
  boolean setFunctionModelMapping(AiFunctionType functionType, Long modelId);

  /**
   * 查询所有映射(带模型和厂商信息)
   *
   * @return 所有映射列表
   */
  List<AiFunctionModelMapping> listAllMappingsWithModel();
}
