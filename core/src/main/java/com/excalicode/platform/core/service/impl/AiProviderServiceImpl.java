package com.excalicode.platform.core.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.entity.AiModel;
import com.excalicode.platform.core.entity.AiProvider;
import com.excalicode.platform.core.mapper.AiProviderMapper;
import com.excalicode.platform.core.service.AiModelService;
import com.excalicode.platform.core.service.AiProviderService;
import lombok.RequiredArgsConstructor;

/**
 * AI 厂商 Service 实现类
 */
@Service
@RequiredArgsConstructor
public class AiProviderServiceImpl extends ServiceImpl<AiProviderMapper, AiProvider>
        implements AiProviderService {

    private final AiModelService aiModelService;

    @Override
    public AiProvider getProviderWithModels(Long providerId) {
        AiProvider provider = this.getById(providerId);
        if (provider != null) {
            provider.setModels(aiModelService.listByProviderId(providerId));
            maskApiKey(provider);
        }
        return provider;
    }

    @Override
    public List<AiProvider> listProvidersWithModels() {
        List<AiProvider> providers = this.list();
        providers.forEach(provider -> {
            provider.setModels(aiModelService.listByProviderId(provider.getId()));
            maskApiKey(provider);
        });
        return providers;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeProviderWithModels(Long providerId) {
        aiModelService
                .remove(new LambdaQueryWrapper<AiModel>().eq(AiModel::getProviderId, providerId));
        return this.removeById(providerId);
    }

    /**
     * 掩码 API Key, 防止泄露 只保留前4位和后4位, 中间用 *** 代替
     */
    private void maskApiKey(AiProvider provider) {
        if (provider == null || provider.getApiKey() == null) {
            return;
        }
        String apiKey = provider.getApiKey();
        if (apiKey.length() <= 8) {
            provider.setMaskedApiKey("***");
        } else {
            String masked = apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
            provider.setMaskedApiKey(masked);
        }
        // 清空原始 API Key, 防止返回给前端
        provider.setApiKey(null);
    }
}
