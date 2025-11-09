# Tasks: update-frontend-lint-tooling

- [x] Spike：验证 oxlint/oxfmt 在 Windows + WSL 环境下的兼容性，并确认 `.vue` lint 覆盖策略（原生 or JS 插件）。
- [x] 编写 `frontend/.oxlintrc.jsonc`、`frontend/.oxfmtrc.jsonc`，迁移现有规则/格式设定并补充忽略列表。
- [x] 更新 `frontend/package.json`（依赖 + scripts）与相关配置（如 VSCode settings、`pnpm-workspace.yaml`）以使用 `oxlint`/`oxfmt`。
- [x] 移除旧的 ESLint/Prettier 配置与依赖，确保 `pnpm install` 后不再引入重复工具。
- [x] 在 README / openspec/project.md 等文档中说明新的 lint/format 流程，并更新 CI/workflow 命令。
- [x] 本地与 CI 跑通 `pnpm lint`、`pnpm lint:check`、`pnpm format:check`，修复阻塞性告警后提交。
