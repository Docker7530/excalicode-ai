# EXCALICODE AI 平台

> AI 驱动的需求工程与任务协同平台，集 COSMIC 功能点分析、知识入库与任务编排于一体。

## 产品概览

EXCALICODE AI 平台将需求分析、知识回溯、AI 调用配置和任务分配整合为一套统一的工作台。后端采用 Spring Boot 多模块（core + web），前端为 Vue 3 + Element Plus，AI 能力通过 Spring AI 统一封装，可对接多家模型服务。

## 快速开始

### 1. 安装依赖

- JDK 21、Maven 3.9+
- Node.js 20+ 与 pnpm 10+
- MySQL 8.x、Redis 7.x
- （可选）SiliconFlow 或其他兼容 OpenAI 的模型访问凭据

### 2. 配置环境变量

1. 复制 `.env.example` 为 `.env`，填写数据库、Redis、JWT、`SILICONFLOW_API_KEY` 等变量。

### 3. 初始化数据库

```bash
mysql -u root -p < web/src/main/resources/sql/init-database.sql
```

脚本会创建模型/提示词/任务/用户等核心表并注入默认管理员（用户名 `admin`，初始密码 `admin123`）。

### 4. 启动后端

```bash
# 在仓库根目录
mvn clean install
mvn -pl web -am spring-boot:run
```

### 5. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```
