package com.excalicode.platform.web.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.excalicode.platform.common.enums.AiFunctionType;
import com.excalicode.platform.core.dto.AiFunctionTypeDto;
import com.excalicode.platform.core.dto.SetFunctionMappingRequest;
import com.excalicode.platform.core.entity.AiFunctionModelMapping;
import com.excalicode.platform.core.service.AiFunctionModelMappingService;
import com.excalicode.platform.core.service.ChatModelProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 功能-模型映射管理 Controller
 *
 * 提供功能类型与 AI 模型绑定的管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai-function")
@RequiredArgsConstructor
public class AiFunctionMappingController {

    private final AiFunctionModelMappingService mappingService;
    private final ChatModelProvider chatModelProvider;

    /**
     * 获取所有功能类型枚举
     */
    @GetMapping("/function-types")
    public ResponseEntity<List<AiFunctionTypeDto>> getFunctionTypes() {
        List<AiFunctionTypeDto> functionTypes = Arrays.stream(AiFunctionType.values())
                .map(type -> new AiFunctionTypeDto(type.getCode(), type.getDescription()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(functionTypes);
    }

    /**
     * 获取所有映射关系 (包含模型和厂商信息)
     */
    @GetMapping("/list")
    public ResponseEntity<List<AiFunctionModelMapping>> list() {
        List<AiFunctionModelMapping> mappings = mappingService.listAllMappingsWithModel();
        return ResponseEntity.ok(mappings);
    }

    /**
     * 设置或更新功能类型的模型映射
     */
    @PostMapping("/set")
    public ResponseEntity<Void> setMapping(@RequestBody SetFunctionMappingRequest request) {
        log.info("设置功能映射: functionType={}, modelId={}", request.getFunctionType(),
                request.getModelId());

        AiFunctionType functionType = AiFunctionType.fromCode(request.getFunctionType());
        if (functionType == null) {
            return ResponseEntity.badRequest().build();
        }

        boolean success =
                mappingService.setFunctionModelMapping(functionType, request.getModelId());
        if (success) {
            // 清除缓存，使新配置生效
            chatModelProvider.clearCache();
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除功能映射
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("删除功能映射: id={}", id);
        boolean success = mappingService.removeById(id);
        if (success) {
            // 清除缓存
            chatModelProvider.clearCache();
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 手动清除 ChatModel 缓存
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<Void> clearCache() {
        log.info("手动清除 ChatModel 缓存");
        chatModelProvider.clearCache();
        return ResponseEntity.ok().build();
    }
}
