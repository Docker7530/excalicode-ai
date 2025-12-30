# 自动部署（GitHub Actions）

1. `main` 分支有提交时：构建 `frontend`，把 `frontend/dist` 同步到服务器目录（用于 Nginx 静态托管）
2. 同时构建后端 `web` 模块 Jar：上传到服务器并重启服务（推荐用 systemd）

## 服务器约定目录（建议）

- 前端静态目录：`/var/www/excalicode-ai/`
- 后端 Jar 目录：`/opt/excalicode-ai/app/`（Jar 固定名：`excalicode.jar`）
- systemd 环境变量文件：`/etc/excalicode-ai/excalicode-ai.env`

可以改，但要同步修改仓库的 GitHub Secrets。

## 服务器初始化（systemd）

把仓库里的 `deploy/server/systemd/` 拷贝到服务器任意目录，然后执行：

```bash
sudo bash install.sh
```

它会：

- 创建系统用户 `excalicode`
- 创建目录：`/opt/excalicode-ai/app`、`/opt/excalicode-ai/logs`、`/etc/excalicode-ai`
- 安装服务：`/etc/systemd/system/excalicode-ai.service`
- 生成环境变量文件（仅首次）：`/etc/excalicode-ai/excalicode-ai.env`

然后必须编辑：`/etc/excalicode-ai/excalicode-ai.env` 填写 DB/Redis/JWT/SILICONFLOW 等配置。

## Nginx 配置

把 `deploy/server/nginx/excalicode-ai.conf` 放到服务器：

```bash
sudo cp excalicode-ai.conf /etc/nginx/sites-available/excalicode-ai
sudo ln -s /etc/nginx/sites-available/excalicode-ai /etc/nginx/sites-enabled/excalicode-ai
sudo nginx -t
sudo systemctl reload nginx
```

说明：

- 前端 SPA：`/` 走 `index.html`
- 后端 API：`/web/**` 反代到 `127.0.0.1:9527`，并去掉 `/web` 前缀

## GitHub Secrets

仓库 Settings → Secrets and variables → Actions → New repository secret：

- `DEPLOY_HOST`：服务器 IP/域名
- `DEPLOY_PORT`：SSH 端口（不填默认 22）
- `DEPLOY_USER`：SSH 用户
- `DEPLOY_SSH_KEY`：该用户的私钥内容（ed25519 推荐）
- `DEPLOY_SSH_KNOWN_HOSTS`：服务器 host key（推荐填，避免中间人攻击；不填会在流水线里自动 `ssh-keyscan`）
- `DEPLOY_FRONTEND_PATH`：服务器前端目录（如 `/var/www/excalicode-ai`）
- `DEPLOY_APP_PATH`：服务器 Jar 目录（如 `/opt/excalicode-ai/app`）
- `DEPLOY_RESTART_CMD`：重启命令（推荐：`sudo systemctl restart excalicode-ai`）

### 生成 SSH Key（在你本地）

```bash
ssh-keygen -t ed25519 -C "github-actions" -f ./id_ed25519
```

- 把 `id_ed25519.pub` 追加到服务器 `DEPLOY_USER` 的 `~/.ssh/authorized_keys`
- 把 `id_ed25519` 的内容粘到 GitHub Secret：`DEPLOY_SSH_KEY`

### 获取 known_hosts（在你本地）

```bash
ssh-keyscan -p 22 -H your.host
```

把输出粘到 GitHub Secret：`DEPLOY_SSH_KNOWN_HOSTS`

## 让 GitHub Actions 能重启服务（sudoers）

如果 `DEPLOY_USER` 不是 root，你需要允许它无密码执行重启命令，否则流水线会卡死。

示例（假设 `DEPLOY_USER=deploy`）：

```bash
sudo tee /etc/sudoers.d/excalicode-ai-deploy >/dev/null <<'EOF'
deploy ALL=(root) NOPASSWD: /bin/systemctl restart excalicode-ai
EOF
sudo chmod 440 /etc/sudoers.d/excalicode-ai-deploy
```

## 工作流文件

- GitHub Actions：`.github/workflows/deploy-main.yml`
- 触发条件：push 到 `main` 或手动 `workflow_dispatch`
