# Project Context

## Purpose
Excalicode AI is an AI-assisted requirements engineering platform that automates the full COSMIC 功能点分析流程。后端聚焦多阶段 AI 调度、Excel 导入导出与文档生成，前端提供需求扩写、功能拆分、假期数据处理与后台模型治理界面，目标是把需求分析团队的手工工作压缩到最短链路。

## Tech Stack
- 后端：Java 21、Spring Boot 3.5、Spring AI、Spring Security、MyBatis-Plus、Caffeine、Jakarta Validation、JWT、Apache POI、Dotenv。
- 前端：Vue 3、Vite 7、Element Plus、Vue Router、Axios、Sass、Vditor、unplugin-auto-import / Components。
- 测试与构建：Maven 3.9+、Vitest、ESLint 9、Prettier 3、npm 10+。
- 基础设施：MySQL 8（utf8mb4）、SiliconFlow OpenAI 兼容接口（默认 deepseek-ai/DeepSeek-V3.1 模型）、SSE 通信、`.env` 环境变量注入。

## Project Conventions

### Code Style
- Java 代码使用 Spring 官方格式，Lombok 只做样板简化，命名遵循领域语义（CosmicProcess、AiFunctionType 等），公共常量置于 `common` 模块集中管理。
- Java 业务层禁止出现三层以上缩进，优先拆函数或调整数据结构；异常全部转化为领域异常类型。
- 前端启用 ESLint（Vue + TS 规则）与 Prettier，`.vue` 文件使用 `<script setup lang=\"ts\">`，Sass 变量统一在 `styles` 目录管理。
- 提示词、SQL、配置均以 UTF-8 存储，并使用中文注释阐明业务背景。

### Architecture Patterns
- Maven 聚合：`common`（工具/提示词/缓存）→ `core`（领域服务、AI 调度、Excel）→ `web`（REST、安全、SSE）。
- 统一入口 `CosmicService` 负责编排多阶段 AI 调用，`AiFunctionExecutor` 根据数据库中功能-模型映射挑选模型，并通过 Spring AI 接通厂商。
- SSE 用于需求扩写流式响应，Excel / Word 由 Apache POI 生成；前端通过 Axios + EventSource 统一请求封装，后台管理使用同一 UI 布局。
- .env 由 DotenvConfig 加载，系统环境优先，避免把密钥写进配置库。

### Testing Strategy
- 后端：使用 Spring Boot Test 对服务层与控制器做单体测试，涉及外部模型的部分使用 WireMock / Mockito 替身；Excel 导入导出提供示例文件做回归。
- 前端：Vitest + Vue Test Utils 覆盖组件与组合式逻辑，`npm run check` 组合 lint+format 检查。
- 集成：手动验收需跑完整 COSMIC 链路（扩写→拆解→子过程→导出），同时验证管理员界面的模型映射和限流配置。

### Git Workflow
- 主分支 `main` 保存可部署状态，特性以 `feature/<slug>` 或 `fix/<slug>` 分支开发，完成后通过 PR 合并。
- 每次提交必须遵循 `<type>(<scope>): <subject>` 规范（type 取自约定列表，subject 小写开头且无句号），多项变更需在正文列出 bullet。
- 严禁强制推送覆盖他人工作，合并前确保 `npm run check` 与 `mvn -pl web -am test` 至少其一通过。

## Domain Context
- 核心业务是 COSMIC 功能点度量：需求扩写、功能过程拆解、子过程推理（单阶段与并发模式）、Excel / Word 导出、模型治理。
- 系统还提供假期数据拆分工具、管理员配置界面，要求 AI 模型可以按功能动态更换并即时生效。
- SSE 输出为纯文本片段，前端在 `onChunk` 中累积展示；Excel 模板需满足规范字段，否则有自动清洗和错误提示。

## Important Constraints
- Never break userspace：现有 COSMIC 流程、Excel 模板、导出格式都视为 API，任何变更必须保持兼容或提供迁移脚本。
- 所有敏感配置（DB、JWT、API Key）只能来源于 `.env` 或系统环境，禁止写入仓库。
- 模型供应商速率有限，服务层通过信号量+限流兜底，新增功能必须评估并复用这些控制。
- 三层缩进视为设计有问题，需要拆分；跨模块调用只允许通过公共接口或服务，禁止直接操作别的模块内部类。

## External Dependencies
- MySQL 8（`excalicode_ai` 库）承载模型映射、用户、流程数据。
- SiliconFlow / OpenAI 兼容接口，依赖 `SILICONFLOW_API_KEY`（或自定义 `OPENAI_API_KEY` + `spring.ai.openai.base-url`）。
- 前端构建依赖 npm registry、Element Plus CDN 图标（本地开发需要良好网络），Excel/Word 功能依赖 Apache POI。
- Dotenv Java 负责读取 `.env`，Caffeine 提供提示词缓存，JWT 依赖 `jjwt` 套件。
