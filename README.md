# Easy-wiki

轻量化团队知识库与工作协同平台，包含 Spring Boot 后端与 Android Compose 客户端。

## 项目简介

Easy-wiki 面向小团队提供知识库管理、任务协同、群组协作与 AI Agent 辅助能力。本仓库为 monorepo 结构：

```
Easy-wiki/
├── prd/                    # 产品需求文档
├── docs/superpowers/       # 设计文档与实施计划
├── easy-wiki-server/       # Spring Boot 后端
├── easy-wiki-android/      # Android Compose 客户端
├── docker-compose.yml      # 后端 + MySQL 容器化部署
└── README.md
```

## 环境要求

| 工具 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+（推荐 Java 17） | 后端编译与运行 |
| Maven | 3.9+ | 后端构建 |
| MySQL | 8.0 | 本地开发或 Docker 部署 |
| Docker Desktop | 最新版（可选） | 容器化一键部署 |
| Android Studio | 最新稳定版 | Android APK 构建与调试 |

## 本地开发（无 Docker）

适用于未安装 Docker 或希望直接在本机调试后端的场景。

### 1. 安装并配置 MySQL 8

1. 安装 MySQL 8.0（Windows 可使用 [MySQL Installer](https://dev.mysql.com/downloads/installer/)）。
2. 创建数据库与用户：

```sql
CREATE DATABASE easywiki CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'easywiki'@'localhost' IDENTIFIED BY 'easywiki';
GRANT ALL PRIVILEGES ON easywiki.* TO 'easywiki'@'localhost';
FLUSH PRIVILEGES;
```

默认连接配置见 `easy-wiki-server/src/main/resources/application.yml`：

- 地址：`localhost:3306`
- 数据库：`easywiki`
- 用户名 / 密码：`easywiki` / `easywiki`

### 2. 创建上传目录

后端默认将文件保存到 `D:/easy-wiki/uploads/`，启动前需手动创建：

**Windows（PowerShell）：**

```powershell
mkdir D:\easy-wiki\uploads\
```

**Linux / macOS：**

```bash
mkdir -p /path/to/your/uploads/
export UPLOAD_PATH=/path/to/your/uploads/
```

### 3. 配置环境变量（可选）

本地开发可使用 `application.yml` 中的默认值。如需覆盖，在启动前设置：

```powershell
# Windows PowerShell 示例
$env:JWT_SECRET = "your-production-secret-at-least-32-chars"
$env:UPLOAD_PATH = "D:/easy-wiki/uploads/"
$env:DEEPSEEK_API_KEY = "sk-xxx"
$env:AGENT_ENABLED = "false"
$env:FCM_CREDENTIALS = "D:/path/to/firebase-service-account.json"
```

### 4. 启动后端

```bash
cd easy-wiki-server
mvn spring-boot:run
```

服务默认监听 `http://localhost:8080`。

### 5. 验证健康检查

```bash
curl http://localhost:8080/api/v1/health
```

预期响应：

```json
{"code":0,"message":"ok","data":{"status":"UP"}}
```

## Docker 部署

需要已安装 Docker Desktop 或 Docker Engine + Docker Compose。

### 启动服务

在项目根目录执行：

```bash
docker compose up --build -d
```

该命令将：

- 启动 MySQL 8.0（数据库 `easywiki`，用户 `easywiki` / `easywiki`）
- 构建并启动 Spring Boot 应用（端口 `8080`）
- 持久化 MySQL 数据与上传文件（Docker 卷 `mysql-data`、`upload-data`）

### 验证部署

```bash
curl http://localhost:8080/api/v1/health
```

### 停止服务

```bash
docker compose down
```

如需同时删除数据卷（**会清空数据库与上传文件**）：

```bash
docker compose down -v
```

### Docker 环境变量

可通过 `.env` 文件或 shell 环境变量传入，例如在项目根目录创建 `.env`：

```env
JWT_SECRET=your-production-secret-at-least-32-chars
DEEPSEEK_API_KEY=sk-xxx
AGENT_ENABLED=false
```

容器内 `UPLOAD_PATH` 固定为 `/data/uploads`（已通过卷 `upload-data` 持久化）。

## Android 客户端构建

1. 用 **Android Studio** 打开 `easy-wiki-android/` 目录。
2. 等待 Gradle 同步完成。
3. 配置后端地址：
   - 模拟器访问本机后端：`http://10.0.2.2:8080`
   - 真机访问：使用电脑局域网 IP，如 `http://192.168.1.100:8080`
4. 连接设备或启动模拟器，点击 Run 运行 App。

命令行构建 Debug APK：

```bash
cd easy-wiki-android
./gradlew assembleDebug
```

APK 输出路径：`easy-wiki-android/app/build/outputs/apk/debug/app-debug.apk`

## 环境变量

| 变量 | 说明 | 本地开发默认值 | Docker 默认值 |
|------|------|----------------|---------------|
| `JWT_SECRET` | JWT 签名密钥（生产环境至少 32 字符） | `dev-secret-change-in-production-min-32-chars!!` | 同左，可通过 `.env` 覆盖 |
| `UPLOAD_PATH` | 文件上传存储目录 | `D:/easy-wiki/uploads/` | `/data/uploads` |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥（启用 Agent 时必填） | 空 | 空，通过 `.env` 设置 |
| `AGENT_ENABLED` | 是否启用 AI Agent 功能 | `false` | `false` |
| `FCM_CREDENTIALS` | Firebase 服务账号 JSON 文件路径（推送通知） | — | 暂未挂载，后续按需配置 |

Spring Boot 还支持标准数据源环境变量（Docker Compose 已配置）：

| 变量 | 说明 |
|------|------|
| `SPRING_DATASOURCE_URL` | JDBC 连接 URL |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 |

## 上传目录说明

- **本地开发**：默认路径为 `D:/easy-wiki/uploads/`，首次启动前须执行 `mkdir D:/easy-wiki/uploads/`（Windows）或设置 `UPLOAD_PATH` 指向已存在的目录。
- **Docker 部署**：容器内路径为 `/data/uploads`，数据通过命名卷 `upload-data` 持久化，重启容器不会丢失已上传文件。
- 确保运行用户对上传目录具有读写权限。

## 相关文档

- PRD：`prd/prd-v1.0.md`
- 概要设计：`docs/superpowers/specs/2026-07-06-easy-wiki-design.md`
- 实施计划：`docs/superpowers/plans/2026-07-06-easy-wiki-v1-implementation.md`
