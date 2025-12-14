package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.ai.SetFunctionPromptMappingRequest;
import com.excalicode.platform.core.entity.AiFunctionPromptMapping;
import com.excalicode.platform.core.service.entity.AiFunctionPromptMappingService;
import java.util.List;
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

/** 功能-提示词映射管理 Controller 提供功能与提示词模板绑定的管理接口 */
@Slf4j
@RestController
@RequestMapping("/api/function-prompts")
@RequiredArgsConstructor
public class FunctionPromptMappingController {

  private final AiFunctionPromptMappingService mappingService;

  /** 获取所有映射关系 (包含提示词模板信息) */
  @GetMapping
  public ResponseEntity<List<AiFunctionPromptMapping>> list() {
    List<AiFunctionPromptMapping> mappings = mappingService.listAllMappingsWithPrompt();
    return ResponseEntity.ok(mappings);
  }

  /** 根据功能代码查询提示词代码 */
  @GetMapping("/{functionCode}")
  public ResponseEntity<String> getPromptCodeByFunctionCode(@PathVariable String functionCode) {
    String promptCode = mappingService.getPromptCodeByFunctionCode(functionCode);
    if (promptCode != null) {
      return ResponseEntity.ok(promptCode);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  /** 设置或更新功能的提示词映射 */
  @PostMapping
  public ResponseEntity<Void> setMapping(@RequestBody SetFunctionPromptMappingRequest request) {
    log.info(
        "设置功能-提示词映射: functionCode={}, promptCode={}",
        request.getFunctionCode(),
        request.getPromptCode());

    boolean success =
        mappingService.setFunctionPromptMapping(request.getFunctionCode(), request.getPromptCode());
    if (success) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.internalServerError().build();
    }
  }

  /** 删除功能-提示词映射 */
  @DeleteMapping("/{functionCode}/{promptCode}")
  public ResponseEntity<Void> delete(
      @PathVariable String functionCode, @PathVariable String promptCode) {
    log.info("删除功能-提示词映射: functionCode={}, promptCode={}", functionCode, promptCode);
    boolean success = mappingService.deleteFunctionPromptMapping(functionCode, promptCode);
    if (success) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }
}
