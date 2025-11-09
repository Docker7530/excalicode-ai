# Proposal: update-frontend-lint-tooling

## 背景
- 前端当前依赖 ESLint Flat Config + Prettier（参见 `frontend/eslint.config.js`、`frontend/prettier.config.js`），脚本通过 `pnpm lint`、`pnpm format` 触发。规则覆盖 Vue/TS/JS，但执行耗时长、依赖链大，且与后续 Oxidation Compiler 生态割裂。
- oxlint/oxfmt 提供 Rust 实现的 ESLint/Prettier 兼容能力，官方文档已经给出 JSONC 配置、插件以及 CLI 使用方式（参考 [Configuring Oxlint](https://oxc.rs/docs/guide/usage/linter/config.html)、[Oxlint Config Reference](https://oxc.rs/docs/guide/usage/linter/config-file-reference.html)、[Formatter Guide](https://oxc.rs/docs/guide/usage/formatter.html) 以及 `npm/oxlint`、`npm/oxfmt` schema）。
- 我们希望切换到 OXC 工具链，获得更快执行、统一跨项目规则，同时保持与现有 COSMIC 业务、Vue 代码风格兼容，且不破坏当前开发体验。

## 需求概述
1. 在 `frontend/` 中落地 `oxlint`、`oxfmt` 配置文件，体现我们项目的命名、导入、风格偏好。
2. 用 `oxlint` 取代 ESLint，覆盖 TypeScript、JavaScript、Vue SFC（至少脚本块）与 Vitest 测试；阻断未覆盖文件的回归。
3. 用 `oxfmt` 取代 Prettier，继承现有 print width / quote / semi 等约束，同时利用新特性（如 `objectWrap`、`experimentalSortImports` 可选开关）。
4. 更新依赖与 NPM Script（含 CI 任务，如果有）以使用新的 CLI。
5. 文档（README / CONTRIBUTING / 开发指引）同步更新，帮助团队完成工具切换。

## 目标
1. **配置产物**：在 `frontend/.oxlintrc.jsonc`、`frontend/.oxfmtrc.jsonc` 中定义官方推荐、可维护的设置，包含分类（`categories`）、插件（`plugins`、`jsPlugins`）、忽略列表、规则 overrides。
2. **脚本替换**：`pnpm lint`/`test` 流程使用 `oxlint`，`pnpm format` 改为 `oxfmt`，CI 亦统一（必要时增加 `--no-cache`、`--format` 选项）。
3. **依赖修剪**：移除 ESLint/Prettier 相关依赖与配置文件，添加 `oxlint`、`oxfmt`、潜在 JS 插件（如 `eslint-plugin-vue`）与其 schema 引用。
4. **Vue 支持**：验证 `oxlint` 对 `.vue` 的 lint 覆盖。若需 JS 插件（基于 `jsPlugins` API）加载 `eslint-plugin-vue`、`vue-eslint-parser`，则明确其 wiring。若当前版本仍不稳定，需文档化 fallback（如对 `.vue` 保留 ESLint 子任务），并给出迁移里程碑。
5. **对齐团队规范**：保持现有命名/导入限制（如 `no-console` 仅 warn、`eqeqeq` error）以及格式（80 列、单引号、分号、trailing comma）。

## 非目标
- 不修改后端、Docs、CI 以外区域（除非触发新的 lint/format 步骤）。
- 不在此变更中引入 lint-staged、pre-commit 等额外工具（如需另行提案）。
- 不处理与 OXC 无关的依赖升级。

## 方案概览

### oxlint 配置策略
- **基本结构**：使用 `.oxlintrc.jsonc`，挂上 `$schema: "./node_modules/oxlint/configuration_schema.json"` 以获得编辑器提示（依据 `npm/oxlint/configuration_schema.json`）。
- **启用插件**：`["oxc","typescript","unicorn","import","promise","node","vitest","jsdoc"]`，确保 TS、Node、Vitest、Promise 语义都被覆盖；若验证 `.vue` 依赖 JS 插件，则在 `jsPlugins` 中声明 `["eslint-plugin-vue"]` 并设置 `parserOptions`/rules 兼容。
- **分类级别**：`categories.correctness/perf = "error"`，`categories.suspicious/style = "warn"`，其余默认关闭，满足官方推荐中“错误即阻断，风格渐进”原则。
- **忽略与覆盖**：继承现有忽略列表（`dist/**`, `node_modules/**`, `*.d.ts`, `auto-imports.d.ts`, `components.d.ts`, `src/api/**/__mocks__` 等），并为 Vitest / Playwright / 配置脚本提供 `overrides`（如测试允许 devDependencies、禁用 `no-unused-expressions`）。
- **规则精选**：迁移原来的团队约束（`no-console`, `eqeqeq`, `prefer-const`, `no-var`, `import/order` 等），结合 oxlint 内建规则（`unicorn/no-null`, `promise/no-return-in-finally`）。所有 rule 名称与严重级别将根据官方配置指南映射。
- **Vue SFC 流程**：优先尝试 `oxlint --js-plugin eslint-plugin-vue --jsx-a11y-plugin`（文档《JS Plugins》）。若 Windows 环境触发 OOM（已被官方标注），则：1）在 proposal 中新增 spike 任务验证；2）必要时保留 ESLint 仅 lint `.vue` 的“兼容脚本”，直到官方修复（Never break userspace）。

### oxfmt 配置策略
- **.oxfmtrc.jsonc** 继承 Prettier 设定：`printWidth: 80`、`tabWidth: 2`、`singleQuote: true`、`semi: true`、`trailingComma: "all"、`bracketSpacing: true`、`bracketSameLine: false`、`arrowParens: "always"`、`endOfLine: "lf"`（利用 schema options，参考 `npm/oxfmt/configuration_schema.json`）。
- **附加开关**：根据 schema 评估 `objectWrap: "preserve"`、`embeddedLanguageFormatting: "auto"`、`experimentalSortImports`（默认关闭，避免破坏现有导入顺序）。
- **忽略列表**：同步 `.prettierignore` 的 glob（`dist/**`, `node_modules/**`, `reports/**` 等），确保行为一致。

### 依赖 & 脚本
- `package.json`：移除 ESLint/Prettier 相关依赖（含插件、配置包、`@eslint/js`、`eslint-plugin-vue` 如被 oxlint JS 插件复用则保留 devDep），新增 `oxlint`, `oxfmt`, `@oxlint/cli`（若有拆包），并在 `scripts` 中定义：
  - `lint`: `oxlint "src/**/*.{ts,js,vue}" --fix`（fix 由 CLI `--fix` 或 pipeline flag 控制）。
  - `lint:check`: 无 `--fix` 版本供 CI 使用。
  - `format`: `oxfmt \"src/**/*.{ts,js,vue,scss}\"`.
  - `format:check`: 只校验不修改。
- 若需 `pnpm dlx` 执行（不全局安装），需在 README 中说明。

### 迁移步骤（高层）
1. 复制/转换现有 lint/format 规则到新的 JSONC 文件，删除旧配置。
2. 安装依赖、更新脚本，验证 `pnpm lint`, `pnpm lint:check`, `pnpm format`, `pnpm format:check` 行为。
3. 针对 `.vue`、`vitest`、`vite` 配置文件运行 `oxlint` 并修复首轮错报，记录必要规则豁免。
4. 更新 README / openspec/project.md 中的“代码规范”章节，引导大家使用新命令。

## 风险与缓解
- **Vue SFC 支持不完善**：JS 插件仍在技术预览，可能出现 Windows OOM。→ 预先在 Windows + WSL 双环境验证；必要时为 `.vue` 文件保留原 ESLint 配置并标注为临时兼容层，确保不会阻断交付。
- **规则差异导致误报**：oxlint 的规则集合不同于 ESLint，一次性开启 `categories.style` 可能引入大量告警。→ 先按 `correctness/perf` 收紧，`style` 用 `warn` 并设置豁免列表，逐步演进。
- **格式化兼容性**：oxfmt 尚在 WIP，某些语法可能暂不支持。→ 在提案中要求保留 `pnpm format:prettier` 的隐藏逃生脚本（仅供紧急 fallback，文档化），待验证覆盖率后再彻底移除。
- **CI 集成**：GitHub/GitLab workflow 需同步改命令，否则会出现缺少 CLI 的错误。→ 在实施阶段更新 workflow 并增加缓存策略。

## 验收标准
1. `pnpm lint` / `pnpm lint:check` 成功运行在 Windows + WSL + Linux CI，且默认扫描 `.ts/.js/.vue/.json/.config.*`。
2. `pnpm format` / `pnpm format:check` 对项目所有受管文件给出稳定结果，与旧版 Prettier 对比无破坏性差异（重点关注模板/Markdown/Vue SFC）。
3. README（或 CONTRIBUTING）中新增“使用 oxlint/oxfmt”章节；`openspec/project.md` “Code Style” 段落同步工具名称。
4. 所有旧的 ESLint/Prettier 依赖及配置文件全部删除或标记为 deprecated。

## 时间与交付
- 研究 & Spike（含 Vue/Windows 兼容性验证）：1 天。
- 配置/脚本迁移 & 修复首轮 lint：1 天。
- 文档与 CI 更新：0.5 天。
- 缓冲 / Review：0.5 天。

## 参考
- Oxlint 官方文档：[Configuring Oxlint](https://oxc.rs/docs/guide/usage/linter/config.html)、[Configuration File Reference](https://oxc.rs/docs/guide/usage/linter/config-file-reference.html)、[CLI 指南](https://oxc.rs/docs/guide/usage/linter/cli.html)、[JS Plugins](https://oxc.rs/docs/guide/usage/linter/js-plugins.html)。
- Oxfmt 官方文档与 schema：[Formatter Guide](https://oxc.rs/docs/guide/usage/formatter.html)、[`npm/oxfmt/configuration_schema.json`](https://raw.githubusercontent.com/oxc-project/oxc/main/npm/oxfmt/configuration_schema.json)。
- Oxlint schema 示例：[`npm/oxlint/configuration_schema.json`](https://raw.githubusercontent.com/oxc-project/oxc/main/npm/oxlint/configuration_schema.json) 与官方 `oxlintrc.json`。
