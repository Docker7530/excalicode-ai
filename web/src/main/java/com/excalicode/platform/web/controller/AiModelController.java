package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.entity.AiModel;
import com.excalicode.platform.core.service.entity.AiModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 模型管理 Controller */
@Slf4j
@RestController
@RequestMapping("/api/ai-model")
@RequiredArgsConstructor
public class AiModelController {

  private final AiModelService aiModelService;

  /** 新增模型 */
  @PostMapping
  public ResponseEntity<AiModel> create(@RequestBody AiModel model) {
    if (model.getSupportsJsonSchema() == null) {
      model.setSupportsJsonSchema(Boolean.TRUE);
    }
    aiModelService.save(model);
    return ResponseEntity.ok(model);
  }

  /** 更新模型 */
  @PutMapping("/{id}")
  public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody AiModel model) {
    AiModel existing = aiModelService.getById(id);
    model.setId(id);
    if (model.getSupportsJsonSchema() == null && existing != null) {
      model.setSupportsJsonSchema(existing.getSupportsJsonSchema());
    }
    aiModelService.updateById(model);
    return ResponseEntity.ok().build();
  }

  /** 删除模型 (逻辑删除) */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    aiModelService.removeById(id);
    return ResponseEntity.ok().build();
  }
}
