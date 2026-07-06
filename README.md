# Easy-wiki

轻量化团队知识库与工作协同平台。

## 工程结构

```
Easy-wiki/
├── prd/                    # 产品需求文档
├── docs/superpowers/       # 设计文档与实施计划
├── easy-wiki-server/       # Spring Boot 后端
├── easy-wiki-android/     # Android Compose 客户端
├── docker-compose.yml      # 后端 + MySQL 部署
└── README.md
```

## 环境要求

- JDK 17
- Maven 3.8+
- Docker Desktop（可选，用于容器化部署）
- Android Studio（Android 客户端开发）
- MySQL 8.0（本地开发可选，也可用 Docker）

## 后端开发

```bash
cd easy-wiki-server
mvn spring-boot:run
```

健康检查：`http://localhost:8080/actuator/health`

### 环境变量

| 变量 | 说明 | 开发默认值 |
|------|------|------------|
| `JWT_SECRET` | JWT 签名密钥 | 见 application.yml |
| `UPLOAD_PATH` | 文件上传目录 | `D:/easy-wiki/uploads/` |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | — |
| `AGENT_ENABLED` | 是否启用 Agent | `false` |
| `FCM_CREDENTIALS` | Firebase 服务账号 JSON 路径 | — |

## Docker 部署

```bash
docker compose up --build -d
curl http://localhost:8080/actuator/health
```

## Android 开发

1. 用 Android Studio 打开 `easy-wiki-android/`
2. 同步 Gradle
3. 首次启动配置服务端地址（如 `http://10.0.2.2:8080` 模拟器访问本机）
4. 运行 App

```bash
cd easy-wiki-android
./gradlew assembleDebug
```

## 文档

- PRD：`prd/prd-v1.0.md`
- 概要设计：`docs/superpowers/specs/2026-07-06-easy-wiki-design.md`
- 实施计划：`docs/superpowers/plans/2026-07-06-easy-wiki-v1-implementation.md`
