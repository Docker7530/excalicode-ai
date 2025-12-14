package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.ai.SetFunctionMappingRequest;
import com.excalicode.platform.core.entity.AiFunctionModelMapping;
import com.excalicode.platform.core.enums.AiFunctionType;
import com.excalicode.platform.core.service.entity.AiFunctionModelMappingService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 功能-模型映射管理 Controller 提供功能类型与 AI 模型绑定的管理接口 */
@Slf4j
@RestController
@RequestMapping("/api/ai-function")
@RequiredArgsConstructor
public class AiFunctionMappingController {

  private final AiFunctionModelMappingService mappingService;

  /** 获取所有映射关系 (包含模型和厂商信息) */
  @GetMapping("/list")
  public ResponseEntity<List<AiFunctionModelMapping>> list() {
    List<AiFunctionModelMapping> mappings = mappingService.listAllMappingsWithModel();
    return ResponseEntity.ok(mappings);
  }

  /** 设置或更新功能类型的模型映射 */
  @PostMapping("/set")
  public ResponseEntity<Void> setMapping(@RequestBody SetFunctionMappingRequest request) {
    log.info(
        "设置功能映射: functionType={}, modelId={}", request.getFunctionType(), request.getModelId());

    Optional<AiFunctionType> aiFunctionType = AiFunctionType.fromCode(request.getFunctionType());
    if (aiFunctionType.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }
    AiFunctionType functionType = aiFunctionType.get();

    boolean success = mappingService.setFunctionModelMapping(functionType, request.getModelId());
    if (success) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.internalServerError().build();
    }
  }

  /** 删除功能映射 */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    log.info("删除功能映射: id={}", id);
    boolean success = mappingService.removeById(id);
    if (success) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }
}
