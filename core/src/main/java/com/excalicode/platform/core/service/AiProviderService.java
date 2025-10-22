package com.excalicode.platform.core.service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.excalicode.platform.core.entity.AiProvider;

/**
 * AI 厂商 Service 接口
 */
public interface AiProviderService extends IService<AiProvider> {

    /**
     * 根据厂商ID查询厂商及其关联的所有模型
     *
     * @param providerId 厂商ID
     * @return 包含模型列表的厂商信息
     */
    AiProvider getProviderWithModels(Long providerId);

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
     * @return 删除是否成功
     */
    boolean removeProviderWithModels(Long providerId);

}
