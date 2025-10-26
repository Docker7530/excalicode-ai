# EXCALICODE AI 平台

## 项目简介

- 面向需求工程团队的 AI 辅助平台，聚焦 COSMIC 功能点分析和需求文档生成。
- 后端基于 Spring Boot 3.5 + Spring AI，实现按功能动态切换大模型、流式推送和限流控制。
- 前端采用 Vue 3 + Vite + Element Plus，提供分阶段的需求分析流程、休假数据拆分工具和后台配置界面。

## 功能亮点

- COSMIC 全流程：需求扩写 → 功能过程拆解 → 子过程生成（两种策略）→ 表格导出 → Word 文档预览与终稿导出。
- Excel 互操作：支持导入功能过程模板、COSMIC 子过程表，自动清洗并支持批量下载。
- 模型治理：管理员界面可维护 AI 厂商 / 模型清单，并为不同业务功能绑定最合适的模型，实时生效。
- 安全控制：JWT 登录、BCrypt 存储、方法级鉴权、Caffeine 缓存提示词、本地 .env 加载敏感配置。

## 架构总览

```text
excalicode-ai/
├─ pom.xml                  # 聚合工程，统一依赖与插件
├─ common/                  # 通用组件（提示词、异常、缓存服务）
├─ core/                    # 领域服务（COSMIC、AI 调度、Excel）
├─ web/                     # Spring Boot Web 层、REST 接口、安全配置
└─ frontend/                # Vue 3 单页应用，Vite 构建
```

## 核心模块

- `common`: 统一的提示词加载服务、业务异常、枚举定义，结合 Spring Cache 提升提示词读取效率。
- `core`:
    - `CosmicService` 负责多阶段 AI 调用、Excel 导入导出、重复项修复与文档生成。
    - `AiFunctionExecutor` 聚合提示词、模型、厂商配置，按功能统一调度 AI 并处理 JSON Schema。
    - MyBatis-Plus 实体与 Mapper 层封装常用基础数据操作。
- `web`: Spring MVC 控制器、JWT 过滤链、全局异常处理、.env 自动加载、接口分发与静态资源配置。
- `frontend`: Vue Router 多页面（需求分析、休假处理、后台管理），结合 Element Plus UI、SSE 流式渲染、Axios 统一错误处理。

## 技术栈

- 后端：Java 21、Spring Boot 3.5、Spring AI、MyBatis-Plus、Spring Security、Caffeine、Jakarta Validation、JWT。
- 前端：Vue 3、Vite 7、Element Plus、Axios、Vitest、Sass、unplugin-auto-import / Components。
- 基础设施：MySQL 8.x、SiliconFlow OpenAI 兼容接口（默认模型 deepseek-ai/DeepSeek-V3.1）、`.env` 环境变量。

## 环境准备

- JDK 21+
- Maven 3.9+
- Node.js 18+（建议 20 LTS）与 npm
- MySQL 8.x（字符集 `utf8mb4`）
- 可用的 SiliconFlow API Key（或自定义 OpenAI 兼容模型）

## 快速开始

### 1. 克隆与环境配置

```bash
git clone <your-repo-url>
cd excalicode-ai
copy .env.example .env   # Windows PowerShell 可使用 Copy-Item
```

- `.env` 里补齐数据库、JWT 配置（详见下文）。
- 额外在系统环境或 `.env` 中配置 `SILICONFLOW_API_KEY`，供 Spring AI 读取。

### 2. 初始化数据库

```bash
mysql -u root -p < web/src/main/resources/sql/init-database.sql
```

- 会创建 `excalicode_ai` 数据库、AI 厂商/模型/映射/系统用户表，并导入默认 `admin` 账户（密码 `admin123`）。

### 3. 启动后端

```bash
mvn -pl web -am spring-boot:run
```

- 默认监听 `http://localhost:9527`。
- `spring.ai.openai.base-url` 默认指向 `https://api.siliconflow.cn`，如需改用自建或官方 OpenAI，修改
  `application.properties` 或环境变量。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

- Dev Server 默认 `http://localhost:3000`，代理 `/web` 到后端。
- 生产部署前可设置 `VITE_API_BASE_URL`（默认为 `/web`），确保与后端上下文一致。

### 5. 构建与发布

- 后端：`mvn clean package`，产出 `web/target/excalicode.jar`。
- 前端：`npm run build`，产出 `frontend/dist`，可由 Nginx/静态服务器托管或挂载到后端静态目录。

## 环境变量说明

- 数据库：`DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD`
- JWT：`JWT_SECRET`（>=32 字节随机串），`JWT_EXPIRATION`（毫秒）
- 模型：`SILICONFLOW_API_KEY` 或 `OPENAI_API_KEY`（与 `spring.ai.openai.base-url` 配合）
- 前端：`VITE_API_BASE_URL`（可选，自定义 API 前缀）
- 所有变量可放在根目录 `.env`，`DotenvConfig` 会在启动时加载，系统环境优先。

## 主要接口速览

| 模块     | 方法   | 路径                             | 说明            |
|--------|------|--------------------------------|---------------|
| 认证     | POST | `/api/auth/login`              | 账号密码登陆，返回 JWT |
| COSMIC | POST | `/api/requirement/enhance`     | SSE 流式扩写需求    |
| COSMIC | POST | `/api/process/breakdown`       | 功能过程拆解        |
| COSMIC | POST | `/api/cosmic/analyze`          | 子过程生成（单阶段）    |
| COSMIC | POST | `/api/cosmic/analyze-v2`       | 子过程生成（并发两阶段）  |
| COSMIC | POST | `/api/cosmic/table/export`     | 导出 Excel 表    |
| COSMIC | POST | `/api/cosmic/documents/export` | 导出 Word 终稿    |
| COSMIC | POST | `/api/cosmic/process/import`   | 导入功能过程 Excel  |
| 管理     | GET  | `/api/ai-provider/list`        | 查看厂商+模型       |
| 管理     | POST | `/api/ai-function/set`         | 功能 → 模型映射     |
| 管理     | REST | `/api/admin/users/**`          | 管理员 CRUD      |

> 管理端接口默认需要 `ADMIN` 角色；其余业务端接口需登录携带 `Authorization: Bearer <token>`。

## AI 提示词与模型管理

- 提示词定义位于 `common/src/main/resources/prompts`，由 `AiFunctionConfigurationService` 结合 Spring Cache 自动加载。
- `AiFunctionType` 定义各业务功能（需求扩写、功能拆解、备注修正等），配合数据库映射表可分配不同模型。
- 备注修正请求依靠信号量控制并发，辅以限流重试兜底，规避厂商速率限制。

## 开发注意事项

- 端到端流程依赖数据库及外部大模型服务，调试前确保三者可用。
- 如需扩展新的 AI 功能：新增提示词 → 枚举项 → DTO → `CosmicService`/`VacationService` 实现 → 暴露 REST 接口 → 前端接入。
- SSE 接口 (`/api/requirement/enhance`) 默认推送纯文本片段，前端通过 `onChunk` 累积呈现，注意断流与取消处理。
- 前端默认将构建产物部署在独立静态服务器；若要由后端承载，可将 `frontend/dist` 拷贝至 `web/src/main/resources/static`。
