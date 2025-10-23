package com.excalicode.platform.core.service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.excalicode.platform.core.entity.FunctionPromptMapping;

/**
 * 功能-提示词映射 Service 接口
 */
public interface FunctionPromptMappingService extends IService<FunctionPromptMapping> {

    /**
     * 根据功能代码查询提示词代码
     *
     * @param functionCode 功能代码
     * @return 提示词代码, 如果没有配置则返回 null
     */
    String getPromptCodeByFunctionCode(String functionCode);

    /**
     * 设置功能的提示词映射
     *
     * @param functionCode 功能代码
     * @param promptCode 提示词代码
     * @param priority 优先级
     * @return 是否设置成功
     */
    boolean setFunctionPromptMapping(String functionCode, String promptCode, Integer priority);

    /**
     * 查询所有映射(带提示词模板信息)
     *
     * @return 所有映射列表
     */
    List<FunctionPromptMapping> listAllMappingsWithPrompt();

    /**
     * 删除功能的提示词映射
     *
     * @param functionCode 功能代码
     * @param promptCode 提示词代码
     * @return 是否删除成功
     */
    boolean deleteFunctionPromptMapping(String functionCode, String promptCode);
}
