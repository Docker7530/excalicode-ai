package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.entity.AiProvider;
import com.excalicode.platform.core.service.entity.AiProviderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 厂商管理 Controller */
@Slf4j
@RestController
@RequestMapping("/api/ai-provider")
@RequiredArgsConstructor
public class AiProviderController {

  private final AiProviderService aiProviderService;

  /** 查询所有厂商 (包含关联的模型) */
  @GetMapping("/list")
  public ResponseEntity<List<AiProvider>> list() {
    List<AiProvider> providers = aiProviderService.listProvidersWithModels();
    return ResponseEntity.ok(providers);
  }

  /** 新增厂商 */
  @PostMapping
  public ResponseEntity<AiProvider> create(@RequestBody AiProvider provider) {
    aiProviderService.save(provider);
    return ResponseEntity.ok(provider);
  }

  /** 更新厂商 */
  @PutMapping("/{id}")
  public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody AiProvider provider) {
    provider.setId(id);
    aiProviderService.updateById(provider);
    return ResponseEntity.ok().build();
  }

  /** 删除厂商 (逻辑删除, 会级联删除关联的模型) */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    aiProviderService.removeProviderWithModels(id);
    return ResponseEntity.ok().build();
  }
}
