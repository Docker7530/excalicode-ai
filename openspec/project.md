# Project Context

## Purpose
EXCALICODE AI 是面向需求工程与项目管理团队的 AI 辅助平台。主目标是让 COSMIC 功能点分析、需求扩写、Excel/Word 交付、任务编排全部在线完成，并且允许管理员针对不同业务功能选择最合适的大模型、即时推送到所有使用者。平台既要把复杂的需求拆解流程产品化，也要提供后台治理工具（模型/提示词/任务/用户），保证研发和咨询团队可以在既有流程上快速复制成功经验。

## Tech Stack
- **Backend / Services**：Java 21、Spring Boot 3.5、Spring AI（OpenAI/SiliconFlow 兼容接口）、MyBatis-Plus、Spring Security + JWT、Caffeine Cache、Apache POI、Reactor Flux（SSE）、Dotenv、Lombok。
- **Core Modules**：`common`（通用异常、提示词、枚举）→ `core`（AI 调度、COSMIC 领域服务、Excel/Word 处理、任务域）→ `web`（REST API、Security Filter、全局异常、SSE 推送）。
- **Frontend**：Vue 3 + Composition API、Vite 7、Element Plus、Vue Router、Axios、Vitest、ESLint Flat Config、Prettier、Sass、unplugin-auto-import / Components。
- **Infrastructure & Ops**：MySQL 8（utf8mb4）、SiliconFlow OpenAI 兼容 API、`.env` 注入敏感配置、pnpm 10.x、Maven 3.9+、Nginx/静态服务器托管 `frontend/dist`。

## Project Conventions

### Code Style
- **Java**：包名固定为 `com.excalicode.platform.*`，按 `api/model/entity/service/controller` 划分；Service 层使用 `@Service + @RequiredArgsConstructor`，禁止超过三层嵌套；异常统一抛 `BusinessException` 并在 `GlobalExceptionHandler` 做响应封装；公共常量/模板放在 `common`；Excel/AI 相关常量只允许放在对应 service 内，保持作用域最小。
- **Java Config**：配置类集中在 `web.config`，Jackson/安全/ENV 注入独立类负责；SSE 返回 `Flux<String>`，控制器不做业务逻辑；DTO 校验依赖 `jakarta.validation` 注解。
- **Frontend**：Prettier（80 列、2 空格、单引号、分号、Trailing comma）；ESLint Flat Config + Vue 插件，禁止未使用变量，组件名使用 PascalCase，模板事件/属性统一连字符；API 调用通过 `src/api/request.js` 统一封装，返回 Promise，禁止直接在组件里写裸 Axios。
- **命名约定**：所有 AI 功能类型集中在 `AiFunctionType` 枚举；前端常量放在 `src/constants` 并以 `SCREAMING_SNAKE_CASE` 命名；SSE 事件回调统一命名 `onChunk / onFinish / onError`。

### Architecture Patterns
- **多模块 Maven**：`common` 只放无状态依赖，`core` 实现领域服务，`web` 负责 API、认证和基础设施，严格禁止跨层直接依赖。
- **数据流**：需求 → `CosmicService` 拆成功能过程 → 子过程（单阶段/双阶段）→ Excel/Word 导出；AI Prompt 通过 `AiFunctionExecutor` 根据数据库映射应用对应模型；任务批次/子任务通过 `core.service.task` 管理，`web.controller.task` 暴露接口。
- **前后端解耦**：Vue 单页应用只消费 `/api/**`，SSE 接口使用 `EventSource`；前端 views 代表业务页面，components 只做可复用 UI；Excel/Word 上传下载走 REST/二进制流。
- **配置治理**：`.env` 在启动时由 `DotenvConfig` 注入 Spring Environment；Jackson 配置强制 `GMT+8`、`yyyy-MM-dd HH:mm:ss`，确保导出文档与数据库一致。

### Testing Strategy
- **后端**：核心服务依赖 `spring-boot-starter-test`，要求对复杂算法（COSMIC 拆解、Excel 清洗、AI JSON Schema 解析）写单元测试或 `@SpringBootTest` 级联测试；包含多线程/CompletableFuture 的逻辑必须覆盖异常/边界路径。尚未覆盖的 legacy 服务（如部分任务流程）至少要在 PR 中补 fake 数据场景或明确说明手工验证流程。
- **前端**：使用 Vitest 编写组件/工具函数测试，重点关注 `src/api`、复杂编辑组件（ProcessEditor/ProcessTableEditor）和模型治理页面；E2E 目前依赖手工回归，提交前至少完成「登录 → 需求扩写 → 功能拆解 → Excel 导出 → 后台管理列表」串联验收。
- **联合验证**：SSE、Excel 模板、Word 导出属于跨端功能，任何修改都要在本地环境跑通整条链路并附上样例文件/截图。

### Git Workflow
- **分支策略**：`main` 保持可部署；功能以 `feature/<short-desc>` 建分支，涉及多模块的需求拆成多个分支或使用 PR draft 记录子任务。
- **提交规范**：强制使用 `<type>(<scope>): <subject>`，type 限定在 `feat|fix|refactor|docs|style|perf|chore|ci|revert|build`；scope 使用模块名（如 `core`, `web`, `frontend`, `openspec`）。subject 中文描述，但首字母小写，不加句号。
- **代码评审**：PR 必须列出受影响的接口/前端页面、是否需要数据迁移、回归范围；涉及 spec 改动需先完成 OpenSpec proposal 并通过 `openspec validate --strict`。

## Domain Context
- **COSMIC 功能点分析**：流程包含需求扩写 → 功能过程拆解 → 子过程生成（可并发两阶段）→ 表格导出 → Word 终稿预览；所有列名/模板参照行业标准，导入模板 sheet 名固定为「功能点拆分表」。
- **AI 模型治理**：`AiFunctionType` 代表业务功能，管理员通过后台把功能映射到具体模型（SiliconFlow、OpenAI 或私有接口），前端需要即时刷新 mapping；提示词存储在 `common/resources/prompts`，通过缓存加载。
- **任务调度**：管理员导入 Excel 批量创建任务批次，分配执行人并追踪进度；USER 只看到与自己相关的任务，状态同步；管理员可以重新指派并自动重置状态。
- **假期/数据拆分**：VacationSplitPage 提供数据拆分工具，依赖同一 AI 服务层，需要注意速率限制和并发控制信号量。

## Important Constraints
- 所有敏感配置必须来自 `.env` 或系统环境，绝不提交到仓库；`DotenvConfig` 会在启动时自动加载，缺失变量直接导致启动失败。
- SSE 接口 (`/api/requirement/enhance`) 响应格式稳定为纯文本 chunk，前端依赖逐段渲染，禁止随意改变事件名或包裹 JSON。
- COSMIC Excel 导入严格校验列顺序、Sheet 名和必填字段，任何 schema 调整都需要同步前端校验与示例模板。
- 默认模型基于 SiliconFlow `deepseek-ai/DeepSeek-V3.1`，需要保证基线可用；支持切换其他 OpenAI 兼容端点时，不能破坏现有用户。
- 数据存储要求 `utf8mb4` 和 `Asia/Shanghai` 时区，所有导出/展示都要同样设置，否则会造成计数错误。
- 管理端接口全部受 `ADMIN` 角色保护，组件内必须检查角色；后端 SecurityConfig 采用 JWT 过滤链，不允许跳过鉴权。

## External Dependencies
- **SiliconFlow API**：OpenAI 兼容接口，提供 GPT/DeepSeek 模型，需配置 `SILICONFLOW_API_KEY` 与 `spring.ai.openai.base-url`。
- **MySQL 8.x**：承载模型映射、任务、用户、COSMIC 结果等所有业务数据，字符集 `utf8mb4`，依赖 HikariCP。
- **Excel/Word 模板**：导入/导出依赖固定模板（功能点拆分表、COSMIC 子过程表、Word PRD 模版），位于资源目录或通过前端上传。
- **前端运行环境**：Node.js 18+（推荐 20 LTS）、pnpm 10.x、Vite dev server，生产由 Nginx/静态服务托管并通过 `VITE_API_BASE_URL` 访问后端。
- **观察/监控**：Spring Boot Actuator 暴露基础健康检查；Caffeine 缓存配合后台 CacheManageController 管理缓存条目，避免重复拉取提示词或模型配置。
