# EXCALICODE AI 平台

## 项目简介

- 面向需求工程团队的 AI 辅助平台，聚焦 COSMIC 功能点分析和需求文档生成。
- 后端基于 Spring Boot 3.5 + Spring AI，实现按功能动态切换大模型、流式推送和限流控制。
- 前端采用 Vue 3 + Vite + Element Plus，提供分阶段的需求分析流程、休假数据拆分工具和后台配置界面。

## 功能亮点

- COSMIC 全流程：需求扩写 → 功能过程拆解 → 子过程生成（两种策略）→ 表格导出 → Word 文档预览与终稿导出。
- Excel 互操作：支持导入功能过程模板、COSMIC 子过程表，自动清洗并支持批量下载。
- 任务调度：管理员可批量导入任务并分配执行人，USER 角色可在“我的任务”中实时同步状态。
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
- Node.js 18+（建议 20 LTS）与 pnpm（`corepack enable pnpm`）
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
pnpm install
pnpm run dev
```

- Dev Server 默认 `http://localhost:3000`，代理 `/web` 到后端。
- 生产部署前可设置 `VITE_API_BASE_URL`（默认为 `/web`），确保与后端上下文一致。

### 5. 构建与发布

- 后端：`mvn clean package`，产出 `web/target/excalicode.jar`。
- 前端：`pnpm run build`，产出 `frontend/dist`，可由 Nginx/静态服务器托管或挂载到后端静态目录。

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
| 管理     | POST | `/api/admin/task-batches/import` | Excel 导入任务草稿 |
| 管理     | POST | `/api/admin/task-batches`        | 发布大任务批次并生成子任务 |
| 管理     | GET  | `/api/admin/task-batches`        | 批次总览列表 |
| 管理     | GET  | `/api/admin/task-batches/{id}`   | 查看批次详情与子任务 |
| 管理     | PUT  | `/api/admin/task-batches/{batchId}/tasks/{taskId}/assignee` | 更新子任务执行人 |
| 管理     | GET  | `/api/admin/task-assignees`      | 获取可分配用户 |
| 用户     | GET  | `/api/tasks/my`                  | 用户查看参与的大任务批次 |
| 用户     | GET  | `/api/tasks/my/{batchId}`        | 查看某批次内分配给自己的子任务 |
| 用户     | PATCH | `/api/tasks/{taskId}/status`     | 更新任务状态（未完成/已完成） |

> 管理端接口默认需要 `ADMIN` 角色；其余业务端接口需登录携带 `Authorization: Bearer <token>`。

## AI 提示词与模型管理

- 提示词定义位于 `common/src/main/resources/prompts`，由 `AiFunctionConfigurationService` 结合 Spring Cache 自动加载。
- `AiFunctionType` 定义各业务功能（需求扩写、功能拆解、备注修正等），配合数据库映射表可分配不同模型。
- 备注修正请求依靠信号量控制并发，辅以限流重试兜底，规避厂商速率限制。

## 任务分配与执行流程

1. ADMIN 在“任务分配”中上传包含 `需求标题`、`需求描述`、`计入工作量(人天)` 的 Excel。系统校验列头与空值，并以两行预览展示描述，鼠标悬浮可查看全文。
2. 填写批次标题、说明，为每条草稿指定 USER 执行人后即可发布；发布会创建 `project_task_batch` + `project_task` 双表记录，状态默认为未完成。
3. 管理端批次总览展示完成度、工作量和发布时间，可随时打开批次详情重新指派执行人；重新分配后状态自动重置为“未完成”。
4. USER 端先看到参与的大任务批次，点击进入后仅展示分配给自己的子任务，并可将状态在“未完成 / 已完成”之间切换，结果即时同步到管理员视图。

## 开发注意事项

- 端到端流程依赖数据库及外部大模型服务，调试前确保三者可用。
- 如需扩展新的 AI 功能：新增提示词 → 枚举项 → DTO → `CosmicService`/`VacationService` 实现 → 暴露 REST 接口 → 前端接入。
- SSE 接口 (`/api/requirement/enhance`) 默认推送纯文本片段，前端通过 `onChunk` 累积呈现，注意断流与取消处理。
- 前端默认将构建产物部署在独立静态服务器；若要由后端承载，可将 `frontend/dist` 拷贝至 `web/src/main/resources/static`。
- 前端代码规范统一由 `oxlint` / `oxfmt` 保证，提交前请执行 `pnpm lint:check` 与 `pnpm format:check`（自动修复可用 `pnpm lint`、`pnpm format`）。
