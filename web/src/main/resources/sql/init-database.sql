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
