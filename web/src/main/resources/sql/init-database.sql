-- 创建数据库 (如果不存在)
CREATE DATABASE IF NOT EXISTS excalicode_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE excalicode_ai;

-- 厂商表
CREATE TABLE IF NOT EXISTS ai_provider (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    provider_name VARCHAR(100) NOT NULL COMMENT '厂商名称',
    base_url VARCHAR(500) NOT NULL COMMENT 'API 基础地址',
    api_key VARCHAR(500) NOT NULL COMMENT 'API 密钥',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_provider_name (provider_name),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='厂商表';

-- 模型表
CREATE TABLE IF NOT EXISTS ai_model (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    provider_id BIGINT NOT NULL COMMENT '厂商ID',
    model_name VARCHAR(200) NOT NULL COMMENT '模型名称',
    supports_json_schema TINYINT NOT NULL DEFAULT 1 COMMENT '是否支持 JSON Schema 响应格式: 1-支持, 0-不支持',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_provider_id (provider_id),
    INDEX idx_model_name (model_name),
    INDEX idx_deleted (deleted),
    CONSTRAINT fk_model_provider FOREIGN KEY (provider_id) REFERENCES ai_provider(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型表';

-- 功能-模型映射表
CREATE TABLE IF NOT EXISTS ai_function_model_mapping (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    function_type VARCHAR(50) NOT NULL COMMENT '功能类型代码(对应 AiFunctionType 枚举)',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_function_type (function_type, deleted) COMMENT '功能类型唯一索引',
    INDEX idx_model_id (model_id),
    INDEX idx_deleted (deleted),
    CONSTRAINT fk_mapping_model FOREIGN KEY (model_id) REFERENCES ai_model(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='功能-模型映射表';

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN-管理员, USER-普通用户',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username, deleted) COMMENT '用户名唯一索引',
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 插入默认管理员用户 (密码: admin123, 已使用BCrypt加密)
INSERT INTO sys_user (username, password, role) VALUES
('admin', '$2a$10$jEwInnHpUIdwychSmhY5qeRNiXb/9x64fwCxreTLzM5ipn1MWKVh.', 'ADMIN');

-- 系统设置表（key-value 配置）
CREATE TABLE IF NOT EXISTS sys_setting (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    config_key VARCHAR(200) NOT NULL COMMENT '配置 key（唯一）',
    config_value LONGTEXT NOT NULL COMMENT '配置 value（支持 Markdown 等长文本）',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key, deleted) COMMENT '配置 key 唯一索引',
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统设置表';

-- 首页使用技巧默认内容（可在后台修改）
INSERT INTO sys_setting (config_key, config_value)
VALUES (
  'home.usageTips',
  '## 使用技巧\n\n### 模型与提示词\n\n部分功能已适配 deepseek-chat（DeepSeek-V3.2-Exp 非思考模式），可在【首页后台管理/功能配置】查看相关配置。\n\n📚 参考文档：[提示词教程](https://www.kdocs.cn/l/cdxVmC4hTihU)\n\n### 写需求的最小结构\n\n- 范围：做什么/不做什么\n- 边界：异常、权限、并发、数据量\n- 验收：给出可验证的结果\n'
) ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), deleted = 0;

-- 提示词模板表
CREATE TABLE IF NOT EXISTS ai_prompt_template (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    code VARCHAR(100) NOT NULL COMMENT '提示词唯一标识(如: REQUIREMENT_DOC_GENERATOR)',
    name VARCHAR(200) NOT NULL COMMENT '提示词显示名称',
    content TEXT NOT NULL COMMENT 'Markdown 格式的提示词内容',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code, deleted) COMMENT '提示词代码唯一索引',
    INDEX idx_name (name),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提示词模板表';

-- 功能-提示词映射表
CREATE TABLE IF NOT EXISTS ai_function_prompt_mapping (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    function_code VARCHAR(100) NOT NULL COMMENT '功能标识(对应 AiFunctionType.code)',
    prompt_code VARCHAR(100) NOT NULL COMMENT '提示词代码(对应 ai_prompt_template.code)',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_function_code (function_code, deleted) COMMENT '功能唯一提示词索引',
    INDEX idx_function_code (function_code),
    INDEX idx_prompt_code (prompt_code),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='功能-提示词映射表';

-- 任务批次表（大任务）
CREATE TABLE IF NOT EXISTS project_task_batch (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '批次/大任务标题',
    description TEXT NULL COMMENT '批次说明',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    published_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_batch_creator (created_by),
    INDEX idx_batch_deleted (deleted),
    CONSTRAINT fk_batch_creator FOREIGN KEY (created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务批次表';

-- 任务表（子任务）
CREATE TABLE IF NOT EXISTS project_task (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    batch_id BIGINT NOT NULL COMMENT '所属批次ID',
    title VARCHAR(200) NOT NULL COMMENT '任务标题',
    description TEXT NOT NULL COMMENT '任务描述',
    workload_man_day DECIMAL(10, 2) NOT NULL COMMENT '计入工作量(人天)',
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '任务状态: NOT_STARTED, COMPLETED',
    assignee_id BIGINT NOT NULL COMMENT '执行人ID',
    published_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务发布时间',
    created_by BIGINT NOT NULL COMMENT '发布人ID',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_batch_id (batch_id),
    INDEX idx_assignee_id (assignee_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted),
    CONSTRAINT fk_task_batch FOREIGN KEY (batch_id) REFERENCES project_task_batch(id),
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id),
    CONSTRAINT fk_task_creator FOREIGN KEY (created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务分派表';

-- COSMIC 子过程异步任务表
CREATE TABLE IF NOT EXISTS cosmic_analysis_task (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '提交人ID',
    username VARCHAR(100) NOT NULL COMMENT '提交人用户名',
    status VARCHAR(20) NOT NULL COMMENT '任务状态: PENDING, RUNNING, SUCCEEDED, FAILED',
    request_payload LONGTEXT NOT NULL COMMENT '请求 JSON',
    response_payload LONGTEXT NULL COMMENT '结果 JSON（成功时存储）',
    error_message TEXT NULL COMMENT '失败原因',
    started_time DATETIME NULL COMMENT '开始执行时间',
    finished_time DATETIME NULL COMMENT '完成时间',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_status (status),
    INDEX idx_created_time (created_time),
    CONSTRAINT fk_cosmic_task_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='COSMIC 子过程异步任务';

-- 需求知识库条目表（仅存储，向量化由用户显式触发）
CREATE TABLE IF NOT EXISTS requirement_knowledge_entry (
    document_id VARCHAR(64) NOT NULL COMMENT '文档ID（业务主键）',
    title VARCHAR(128) NOT NULL COMMENT '标题',
    content LONGTEXT NOT NULL COMMENT '正文内容',
    tags VARCHAR(1024) NULL COMMENT '标签（逗号分隔）',
    vectorized TINYINT NOT NULL DEFAULT 0 COMMENT '是否已向量化: 0-否, 1-是',
    vector_updated_time DATETIME NULL COMMENT '向量更新时间',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (document_id),
    INDEX idx_deleted (deleted),
    INDEX idx_updated_time (updated_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='需求知识库条目';
