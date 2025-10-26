package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.dto.PromptTemplateRequest;
import com.excalicode.platform.core.entity.AiPromptTemplate;
import com.excalicode.platform.core.service.PromptTemplateService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提示词模板管理 Controller
 * 提供提示词模板的 CRUD 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/prompt-templates")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    /**
     * 获取所有提示词模板
     */
    @GetMapping
    public ResponseEntity<List<AiPromptTemplate>> list() {
        List<AiPromptTemplate> templates = promptTemplateService.list();
        return ResponseEntity.ok(templates);
    }

    /**
     * 搜索提示词模板
     */
    @GetMapping("/search")
    public ResponseEntity<List<AiPromptTemplate>> search(@RequestParam String keyword) {
        List<AiPromptTemplate> templates = promptTemplateService.search(keyword);
        return ResponseEntity.ok(templates);
    }

    /**
     * 根据 ID 获取提示词模板详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<AiPromptTemplate> getById(@PathVariable Long id) {
        AiPromptTemplate template = promptTemplateService.getById(id);
        if (template != null) {
            return ResponseEntity.ok(template);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 根据 code 获取提示词模板
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<AiPromptTemplate> getByCode(@PathVariable String code) {
        AiPromptTemplate template = promptTemplateService.getByCode(code);
        if (template != null) {
            return ResponseEntity.ok(template);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 创建提示词模板
     */
    @PostMapping
    public ResponseEntity<Void> create(@RequestBody PromptTemplateRequest request) {
        log.info("创建提示词模板: code={}, name={}", request.getCode(), request.getName());

        AiPromptTemplate template = new AiPromptTemplate();
        template.setCode(request.getCode());
        template.setName(request.getName());
        template.setContent(request.getContent());
        template.setDescription(request.getDescription());

        boolean success = promptTemplateService.saveOrUpdatePrompt(template);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 更新提示词模板
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody PromptTemplateRequest request) {
        log.info("更新提示词模板: id={}, code={}, name={}", id, request.getCode(), request.getName());

        AiPromptTemplate template = promptTemplateService.getById(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        template.setCode(request.getCode());
        template.setName(request.getName());
        template.setContent(request.getContent());
        template.setDescription(request.getDescription());

        boolean success = promptTemplateService.saveOrUpdatePrompt(template);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除提示词模板
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("删除提示词模板: id={}", id);
        boolean success = promptTemplateService.removeById(id);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
