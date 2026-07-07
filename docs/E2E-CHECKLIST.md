# Easy-wiki V1.0 端到端验收清单

> 在 `feature/easy-wiki-v1` 分支完成全部 Task 后，按本清单逐项验证。

## 自动化验证

- [ ] **后端单元/集成测试** — `cd easy-wiki-server && mvn test` → 43 tests, 0 failures
- [ ] **后端打包** — `mvn package -DskipTests` → BUILD SUCCESS
- [ ] **健康检查** — 后端启动后 `curl http://localhost:8080/api/v1/health` → `{"code":0,...,"data":{"status":"UP"}}`
- [ ] **Docker 冒烟**（需安装 Docker）— `docker compose up --build -d` 后健康检查通过
- [ ] **Android 单元测试**（需 Android SDK）— `cd easy-wiki-android && ./gradlew test`
- [ ] **Android Debug APK** — `./gradlew assembleDebug` 成功

## 手动 E2E 流程（curl + App）

### 前置条件

- MySQL 8 已运行，数据库 `easywiki` 已创建
- 上传目录已创建：`D:/easy-wiki/uploads/`
- 后端已启动：`cd easy-wiki-server && mvn spring-boot:run`
- （可选 Agent）`AGENT_ENABLED=true` 且 `DEEPSEEK_API_KEY` 已设置
- （可选 FCM）服务端 `FCM_CREDENTIALS` 指向 Firebase 服务账号 JSON；Android 替换 `app/google-services.json`

### 步骤

- [ ] **1. 注册用户 A、B**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@test.com","password":"password123"}'

curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"bob","email":"bob@test.com","password":"password123"}'
```

- [ ] **2. A 登录并创建小组，邀请 B**

```bash
# A 登录，保存 TOKEN_A
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'

# 创建小组（替换 TOKEN_A）
curl -X POST http://localhost:8080/api/v1/groups \
  -H "Authorization: Bearer TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"name":"研发团队","description":"E2E测试","discoverable":true}'

# 创建邀请（替换 GROUP_ID）
curl -X POST http://localhost:8080/api/v1/groups/GROUP_ID/invites \
  -H "Authorization: Bearer TOKEN_A"

# B 登录，通过 invite token 加入
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"bob","password":"password123"}'

curl -X POST http://localhost:8080/api/v1/groups/join/INVITE_TOKEN \
  -H "Authorization: Bearer TOKEN_B"
```

- [ ] **3. A 创建 Wiki 页面，B 收到通知**

```bash
curl -X POST http://localhost:8080/api/v1/groups/GROUP_ID/wiki/pages \
  -H "Authorization: Bearer TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"parentId":null,"title":"入门指南","content":"# Hello Wiki"}'

# B 查看通知
curl http://localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer TOKEN_B"
```

- [ ] **4. A 创建任务并指派 B，B 确认接取**

```bash
curl -X POST http://localhost:8080/api/v1/groups/GROUP_ID/tasks \
  -H "Authorization: Bearer TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"title":"完成E2E","description":"测试","priority":"HIGH","status":"TODO"}'

curl -X POST http://localhost:8080/api/v1/groups/GROUP_ID/tasks/TASK_ID/assign \
  -H "Authorization: Bearer TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"assigneeId":BOB_USER_ID}'

curl -X POST http://localhost:8080/api/v1/groups/GROUP_ID/tasks/TASK_ID/accept \
  -H "Authorization: Bearer TOKEN_B"
```

- [ ] **5. 群聊发送 @ 消息**

```bash
curl -X POST http://localhost:8080/api/v1/groups/GROUP_ID/chat/messages \
  -H "Authorization: Bearer TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"content":"@bob 请看一下 Wiki"}'
```

- [ ] **6. Agent 摘要 Wiki**（需 `AGENT_ENABLED=true`）

```bash
curl -X POST http://localhost:8080/api/v1/groups/GROUP_ID/agent/chat \
  -H "Authorization: Bearer TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"message":"请摘要当前 Wiki 内容"}'
```

- [ ] **7. App 切后台，触发 FCM 推送**（需 Firebase 配置）

  - Android 登录后进入工作区，确认 WebSocket 已连接
  - 将 App 切到后台
  - 另一用户触发通知（如指派任务、@ 提及）
  - 验证系统通知栏收到推送，点击可 Deep Link 跳转

## Android App 手动验证

- [ ] 首次启动配置服务端地址（模拟器：`http://10.0.2.2:8080`）
- [ ] 注册/登录成功
- [ ] 小组列表：创建小组、邀请加入
- [ ] 工作区 Wiki：目录树、详情编辑、409 冲突提示
- [ ] 任务看板：三列 Tab、指派/接取/拒绝
- [ ] 群聊：历史消息 + 实时 WebSocket 推送
- [ ] 通知列表：标已读、点击跳转
- [ ] Agent：对话 Markdown 渲染、任务建议「采纳」

## 已知环境限制（开发机）

| 项目 | 状态 | 说明 |
|------|------|------|
| Docker | 未安装 | Docker Compose 步骤未在本机验证，需自行安装 Docker Desktop |
| JDK | Java 24 | 计划推荐 JDK 17；Java 24 下 Lombok 不可用，后端改用手写 POJO |
| Android SDK | 视环境 | Gradle 构建需 Android Studio / SDK；占位 `google-services.json` 需替换为真实 Firebase 配置 |
