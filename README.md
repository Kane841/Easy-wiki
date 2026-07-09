# Easy-wiki

轻量化团队知识库与工作协同平台 —— 面向小团队的 Wiki + 任务看板 + 群聊 + 通知 + AI Agent。

## 项目进展（2026-07-07）

| 项目 | 状态 |
|------|------|
| **V1.0 MVP 代码** | ✅ 已完成（23/23 Task） |
| **开发分支** | `feature/easy-wiki-v1`（已推送 GitHub） |
| **后端自动化测试** | ✅ 43 tests，0 failures |
| **人工 E2E 验收** | ⏳ 待本机环境验证（MySQL / Android / Firebase / Agent API） |

### 已实现能力

- **后端（Spring Boot 3.2）**：JWT 认证、小组管理、Wiki（乐观锁）、任务看板（指派状态机）、群聊、WebSocket 实时通知、FCM 推送、DeepSeek Agent、定时提醒
- **Android（Compose）**：登录/注册、小组列表、工作区五 Tab（Wiki / 任务 / 聊天 / 通知 / Agent）
- **部署**：Docker Compose + 本地 MySQL 两套方案

> 完整进度见 [`docs/superpowers/progress/2026-07-07-easy-wiki-v1-progress.md`](docs/superpowers/progress/2026-07-07-easy-wiki-v1-progress.md)  
> E2E 验收见 [`docs/E2E-CHECKLIST.md`](docs/E2E-CHECKLIST.md)

---

## 工程结构

```
Easy-wiki/
├── prd/                         # 产品需求文档
├── docs/superpowers/            # 设计、实施计划、进度总结
├── easy-wiki-server/            # Spring Boot 后端
├── easy-wiki-android/           # Android Compose 客户端
├── docker-compose.yml           # MySQL + 后端容器化部署
└── README.md
```

---

## 环境要求

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | **17**（推荐） | Java 24 可用，但 Lombok 不兼容，本项目实体为手写 POJO |
| Maven | 3.9+ | 后端构建与测试 |
| MySQL | 8.0 | 本地开发必装（无 Docker 时） |
| Docker Desktop | 可选 | 容器化一键部署 |
| Android Studio | 可选 | 构建 / 调试 Android 客户端 |

---

## 本地部署与运行（推荐：无 Docker）

### 第一步：准备 MySQL

安装 MySQL 8.0 后，执行：

```sql
CREATE DATABASE easywiki CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'easywiki'@'localhost' IDENTIFIED BY 'easywiki';
GRANT ALL PRIVILEGES ON easywiki.* TO 'easywiki'@'localhost';
FLUSH PRIVILEGES;
```

默认连接（见 `easy-wiki-server/src/main/resources/application.yml`）：

| 项 | 值 |
|----|-----|
| 地址 | `localhost:3306` |
| 数据库 | `easywiki` |
| 用户名 / 密码 | `easywiki` / `easywiki` |

### 第二步：创建上传目录

**Windows（PowerShell）：**

```powershell
mkdir D:\easy-wiki\uploads\
```

**Linux / macOS：**

```bash
mkdir -p ~/easy-wiki/uploads/
export UPLOAD_PATH=~/easy-wiki/uploads/
```

### 第三步：启动后端

```powershell
cd easy-wiki-server

# 可选：运行测试（预期 43 tests PASS）
mvn test

# 启动服务
mvn spring-boot:run
```

服务监听 **http://localhost:8080**。

### 第四步：验证

```powershell
curl http://localhost:8080/api/v1/health
```

预期响应：

```json
{"code":0,"message":"ok","data":{"status":"UP"}}
```

### 第五步：快速 API 冒烟

```powershell
# 注册
curl -X POST http://localhost:8080/api/v1/auth/register `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"alice\",\"email\":\"alice@test.com\",\"password\":\"password123\"}'

# 登录（保存返回的 token）
curl -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"alice\",\"password\":\"password123\"}'
```

更多 curl 用例见 [`docs/E2E-CHECKLIST.md`](docs/E2E-CHECKLIST.md)。

---

## Android 客户端运行

1. 用 **Android Studio** 打开 `easy-wiki-android/`
2. Gradle Sync 完成后 Run
3. **首次启动**配置服务端地址：
   - 模拟器访问本机：`http://10.0.2.2:8080`
   - 真机访问：`http://<电脑局域网IP>:8080`
4. 注册/登录 → 创建小组 → 使用 Wiki / 任务 / 聊天 / 通知 / Agent

命令行构建 Debug APK：

```powershell
cd easy-wiki-android
.\gradlew.bat assembleDebug
```

输出：`easy-wiki-android/app/build/outputs/apk/debug/app-debug.apk`

---

## Docker 部署（可选）

需已安装 Docker Desktop：

```bash
docker compose up --build -d
curl http://localhost:8080/api/v1/health
```

停止服务：`docker compose down`  
清空数据卷：`docker compose down -v`

---

## 环境变量

| 变量 | 说明 | 本地默认 |
|------|------|----------|
| `JWT_SECRET` | JWT 签名密钥（≥32 字符） | 见 `application.yml` |
| `UPLOAD_PATH` | 文件上传目录 | `D:/easy-wiki/uploads/` |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | 空 |
| `AGENT_ENABLED` | 是否启用 AI Agent | `false` |
| `FCM_CREDENTIALS` | Firebase 服务账号 JSON 路径 | — |

**PowerShell 示例（启用 Agent）：**

```powershell
$env:AGENT_ENABLED = "true"
$env:DEEPSEEK_API_KEY = "sk-your-key"
cd easy-wiki-server; mvn spring-boot:run
```

**FCM 推送：** 服务端设置 `FCM_CREDENTIALS`；Android 替换 `easy-wiki-android/app/google-services.json` 为 Firebase 控制台下载的真实配置。

---

## 文档索引

| 文档 | 路径 |
|------|------|
| 产品需求 | `prd/prd-v1.0.md` |
| 概要设计 | `docs/superpowers/specs/2026-07-06-easy-wiki-design.md` |
| 实施计划 | `docs/superpowers/plans/2026-07-06-easy-wiki-v1-implementation.md` |
| 进度总结 | `docs/superpowers/progress/2026-07-07-easy-wiki-v1-progress.md` |
| E2E 验收清单 | `docs/E2E-CHECKLIST.md` |

---

## 已知限制

- **Docker**：未安装时可跳过，使用上文「本地部署」流程
- **Firebase**：仓库内 `google-services.json` 为占位，真实推送需自行配置
- **Agent**：默认关闭，需 `AGENT_ENABLED=true` 及有效 `DEEPSEEK_API_KEY`
