#!/usr/bin/env bash
set -euo pipefail

SERVICE_NAME="excalicode-ai"
APP_USER="excalicode"
APP_GROUP="excalicode"
APP_ROOT="/opt/excalicode-ai"
APP_DIR="${APP_ROOT}/app"
LOG_DIR="${APP_ROOT}/logs"
ENV_DIR="/etc/excalicode-ai"
ENV_FILE="${ENV_DIR}/excalicode-ai.env"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_SRC="${SCRIPT_DIR}/excalicode-ai.service"
ENV_EXAMPLE="${SCRIPT_DIR}/excalicode-ai.env.example"

if [ "$(id -u)" -ne 0 ]; then
  echo "请用 sudo 运行该脚本"
  exit 1
fi

if ! command -v systemctl >/dev/null 2>&1; then
  echo "未找到 systemctl 你的服务器可能不是 systemd 改用自定义脚本方案"
  exit 1
fi

if ! id -u "${APP_USER}" >/dev/null 2>&1; then
  useradd --system --user-group --home-dir "${APP_ROOT}" --create-home --shell /usr/sbin/nologin "${APP_USER}"
fi

mkdir -p "${APP_DIR}" "${LOG_DIR}" "${ENV_DIR}"
chown -R "${APP_USER}:${APP_GROUP}" "${APP_ROOT}"

install -m 0644 "${SERVICE_SRC}" "/etc/systemd/system/${SERVICE_NAME}.service"

if [ ! -f "${ENV_FILE}" ]; then
  install -m 0600 "${ENV_EXAMPLE}" "${ENV_FILE}"
  echo "已生成环境变量文件：${ENV_FILE}（请立即修改真实配置）"
else
  echo "环境变量文件已存在：${ENV_FILE}（跳过覆盖）"
fi

systemctl daemon-reload
systemctl enable "${SERVICE_NAME}"
