# Easy-wiki V1.0 实施计划

> **面向 Agent 执行者：** 必须配合子技能 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans，按 Task 逐步实施。步骤使用复选框（`- [ ]`）语法跟踪进度。

**目标：** 交付 Easy-wiki V1.0 MVP —— 可私有化部署的团队 Wiki + 工作协同平台，包含 Spring Boot 后端、Android Compose 客户端、WebSocket 聊天/通知、FCM 推送、DeepSeek 驱动的 Agent 助手。

**架构：** 分层单体 Spring Boot 3.x（`easy-wiki-server`）+ 独立 Android Compose 应用（`easy-wiki-android`）。REST 负责 CRUD，WebSocket 负责实时聊天/通知，FCM 负责后台推送。所有业务 API 按小组隔离数据。开发环境本地上传路径为 `D:/easy-wiki/uploads/`。

**技术栈：** Java 17、Spring Boot 3.2、Spring Data JPA、MySQL 8、Spring Security + JWT、WebSocket、Firebase Admin SDK、Kotlin、Jetpack Compose、Retrofit、OkHttp、Markwon、DeepSeek（OpenAI 兼容 API）

**设计文档：** `docs/superpowers/specs/2026-07-06-easy-wiki-design.md`

---

## 文件结构总览

### 后端（`easy-wiki-server/`）

| 文件 | 职责 |
|------|------|
| `pom.xml` | Maven 依赖：web、jpa、security、websocket、mysql、jjwt、validation、actuator、test |
| `EasyWikiApplication.java` | 启动入口 |
| `config/SecurityConfig.java` | JWT 过滤器链、公开路由 |
| `config/WebSocketConfig.java` | WebSocket 端点 `/ws` |
| `config/CorsConfig.java` | 允许 Android 开发环境跨域 |
| `security/JwtTokenProvider.java` | 生成/解析 JWT |
| `security/JwtAuthenticationFilter.java` | 提取 Bearer Token |
| `security/GroupMembershipInterceptor.java` | 校验用户是否属于小组 |
| `entity/*.java` | JPA 实体 |
| `repository/*.java` | Spring Data 仓储 |
| `dto/request/*.java` | API 请求体 |
| `dto/response/*.java` | API 响应体 |
| `dto/common/ApiResponse.java` | 统一响应 `{code,message,data}` |
| `service/AuthService.java` | 注册/登录 |
| `service/GroupService.java` | 小组、邀请、入组申请 |
| `service/WikiService.java` | Wiki 目录树、页面、搜索、乐观锁 |
| `service/TaskService.java` | 任务、指派状态机 |
| `service/ChatService.java` | 聊天持久化 + 广播 |
| `service/NotificationService.java` | 持久化 + WebSocket + FCM 分发 |
| `service/FileService.java` | 本地磁盘上传 |
| `service/AgentService.java` | DeepSeek 代理 |
| `agent/DeepSeekClient.java` | OpenAI 兼容 HTTP 客户端 |
| `agent/PromptBuilder.java` | 从 Wiki/任务构建 Prompt |
| `websocket/WsSessionManager.java` | userId → 会话映射 |
| `websocket/WsMessageHandler.java` | 路由 PING/CHAT_MESSAGE |
| `scheduler/TaskReminderScheduler.java` | 截止提醒 + 指派超时 |
| `scheduler/JoinRequestExpiryScheduler.java` | 入组申请 30 天过期 |
| `controller/AuthController.java` | `/api/v1/auth/*` |
| `controller/GroupController.java` | `/api/v1/groups/*` |
| `controller/WikiController.java` | `/api/v1/groups/{gid}/wiki/*` |
| `controller/TaskController.java` | `/api/v1/groups/{gid}/tasks/*` |
| `controller/ChatController.java` | `/api/v1/groups/{gid}/chat/*` |
| `controller/NotificationController.java` | `/api/v1/notifications/*` |
| `controller/DeviceController.java` | `/api/v1/devices` |
| `controller/AgentController.java` | `/api/v1/groups/{gid}/agent/*` |
| `exception/GlobalExceptionHandler.java` | 异常映射为 ApiResponse |
| `exception/ConflictException.java` | Wiki 版本冲突 → 409 |
| `resources/application.yml` | 数据源、JWT、上传路径、Agent 配置 |
| `resources/application-test.yml` | H2 内存库（测试用） |
| `Dockerfile` | Java 17 多阶段构建 |
| `src/test/java/**` | MockMvc + @DataJpaTest |

### Android（`easy-wiki-android/`）

| 文件 | 职责 |
|------|------|
| `app/build.gradle.kts` | Compose、Retrofit、DataStore、FCM、Markwon |
| `data/api/EasyWikiApi.kt` | Retrofit 接口定义 |
| `data/api/AuthInterceptor.kt` | 注入 JWT |
| `data/api/ApiClient.kt` | OkHttp + Retrofit 工厂 |
| `data/ws/WebSocketManager.kt` | 连接/重连/心跳 |
| `data/repository/*Repository.kt` | 按领域封装数据访问 |
| `data/local/SettingsDataStore.kt` | 服务端地址 + Token |
| `model/*.kt` | DTO 模型 |
| `viewmodel/*ViewModel.kt` | UI 状态管理 |
| `ui/navigation/NavGraph.kt` | 应用导航 |
| `ui/auth/LoginScreen.kt` | 登录/注册 |
| `ui/group/GroupListScreen.kt` | 小组列表 + 创建 |
| `ui/workspace/WorkspaceScreen.kt` | 工作台 Tab 壳 |
| `ui/wiki/*` | Wiki 目录树 + 详情 |
| `ui/task/*` | 看板 + 任务详情 |
| `ui/chat/ChatScreen.kt` | 小组群聊 |
| `ui/notification/NotificationScreen.kt` | 通知列表 |
| `ui/agent/AgentScreen.kt` | Agent 对话 |
| `service/FcmService.kt` | FCM 消息处理 |

### 根目录

| 文件 | 职责 |
|------|------|
| `docker-compose.yml` | MySQL + 应用服务 |
| `README.md` | 环境搭建、环境变量、APK 构建说明 |

---

## 阶段 P1：基础骨架（认证 + 小组 + Docker + Android 壳）

### Task 1：搭建 Spring Boot 工程脚手架

**涉及文件：**
- 新建：`easy-wiki-server/pom.xml`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/EasyWikiApplication.java`
- 新建：`easy-wiki-server/src/main/resources/application.yml`
- 新建：`easy-wiki-server/src/test/resources/application-test.yml`
- 测试：`easy-wiki-server/src/test/java/com/easywiki/EasyWikiApplicationTests.java`

- [ ] **步骤 1：编写会失败的上下文加载测试**

```java
// easy-wiki-server/src/test/java/com/easywiki/EasyWikiApplicationTests.java
package com.easywiki;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EasyWikiApplicationTests {
    @Test
    void contextLoads() {}
}
```

- [ ] **步骤 2：运行测试，确认失败**

运行：`cd easy-wiki-server && mvn test -Dtest=EasyWikiApplicationTests`
预期：FAIL — 工程尚不存在

- [ ] **步骤 3：创建 pom.xml 和启动类**

```xml
<!-- easy-wiki-server/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
    </parent>
    <groupId>com.easywiki</groupId>
    <artifactId>easy-wiki-server</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <properties>
        <java.version>17</java.version>
        <jjwt.version>0.12.5</jjwt.version>
    </properties>
    <dependencies>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-websocket</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
        <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId><scope>runtime</scope></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>${jjwt.version}</version></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
        <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin>
        </plugins>
    </build>
</project>
```

```java
// easy-wiki-server/src/main/java/com/easywiki/EasyWikiApplication.java
package com.easywiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EasyWikiApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyWikiApplication.class, args);
    }
}
```

```yaml
# easy-wiki-server/src/main/resources/application.yml
server:
  port: 8080
spring:
  application:
    name: easy-wiki-server
  datasource:
    url: jdbc:mysql://localhost:3306/easywiki?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: easywiki
    password: easywiki
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
easywiki:
  jwt:
    secret: ${JWT_SECRET:dev-secret-change-in-production-min-32-chars!!}
    expiration-ms: 604800000
  upload:
    path: ${UPLOAD_PATH:D:/easy-wiki/uploads/}
  agent:
    enabled: ${AGENT_ENABLED:false}
    api-base-url: https://api.deepseek.com
    api-key: ${DEEPSEEK_API_KEY:}
    model: deepseek-chat
    max-tokens: 8192
    timeout-seconds: 30
```

```yaml
# easy-wiki-server/src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:easywiki_test;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
easywiki:
  jwt:
    secret: test-secret-key-at-least-32-characters-long
    expiration-ms: 3600000
  upload:
    path: ${java.io.tmpdir}/easywiki-test-uploads/
  agent:
    enabled: false
```

- [ ] **步骤 4：运行测试，确认通过**

运行：`cd easy-wiki-server && mvn test -Dtest=EasyWikiApplicationTests`
预期：PASS

- [ ] **步骤 5：提交代码**

```bash
git add easy-wiki-server/
git commit -m "feat(server): scaffold Spring Boot 3.2 project"
```

---

### Task 2：统一 API 响应与全局异常处理

**涉及文件：**
- 新建：`easy-wiki-server/src/main/java/com/easywiki/dto/common/ApiResponse.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/exception/BusinessException.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/exception/ConflictException.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/exception/GlobalExceptionHandler.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/controller/HealthController.java`
- 测试：`easy-wiki-server/src/test/java/com/easywiki/controller/HealthControllerTest.java`

- [ ] **步骤 1：编写会失败的健康检查端点测试**

```java
// easy-wiki-server/src/test/java/com/easywiki/controller/HealthControllerTest.java
package com.easywiki.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void healthReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
}
```

- [ ] **步骤 2：运行测试 — 预期 FAIL**（404）

运行：`cd easy-wiki-server && mvn test -Dtest=HealthControllerTest`

- [ ] **步骤 3：实现 ApiResponse + HealthController + GlobalExceptionHandler**

```java
// easy-wiki-server/src/main/java/com/easywiki/dto/common/ApiResponse.java
package com.easywiki.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

```java
// easy-wiki-server/src/main/java/com/easywiki/exception/BusinessException.java
package com.easywiki.exception;

public class BusinessException extends RuntimeException {
    private final int code;
    public BusinessException(int code, String message) { super(message); this.code = code; }
    public int getCode() { return code; }
}
```

```java
// easy-wiki-server/src/main/java/com/easywiki/exception/ConflictException.java
package com.easywiki.exception;

public class ConflictException extends BusinessException {
    public ConflictException(String message) { super(409, message); }
}
```

```java
// easy-wiki-server/src/main/java/com/easywiki/exception/GlobalExceptionHandler.java
package com.easywiki.exception;

import com.easywiki.dto.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getCode());
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("参数校验失败");
        return ResponseEntity.badRequest().body(ApiResponse.error(400, msg));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
        return ResponseEntity.internalServerError().body(ApiResponse.error(500, "服务器内部错误"));
    }
}
```

```java
// easy-wiki-server/src/main/java/com/easywiki/controller/HealthController.java
package com.easywiki.controller;

import com.easywiki.dto.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP"));
    }
}
```

- [ ] **步骤 4：运行测试 — 预期 PASS**

- [ ] **步骤 5：提交代码**

```bash
git add easy-wiki-server/src/main/java/com/easywiki/dto easy-wiki-server/src/main/java/com/easywiki/exception easy-wiki-server/src/main/java/com/easywiki/controller easy-wiki-server/src/test/java/com/easywiki/controller
git commit -m "feat(server): add unified ApiResponse and exception handler"
```

---

### Task 3：User 实体与 AuthService（注册/登录）

**涉及文件：**
- 新建：`easy-wiki-server/src/main/java/com/easywiki/entity/User.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/repository/UserRepository.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/dto/request/RegisterRequest.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/dto/request/LoginRequest.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/dto/response/AuthResponse.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/service/AuthService.java`
- 测试：`easy-wiki-server/src/test/java/com/easywiki/service/AuthServiceTest.java`

- [ ] **步骤 1：编写会失败的注册测试**

```java
// easy-wiki-server/src/test/java/com/easywiki/service/AuthServiceTest.java
package com.easywiki.service;

import com.easywiki.dto.request.RegisterRequest;
import com.easywiki.entity.User;
import com.easywiki.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {
    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    @Test
    void registerCreatesUserWithHashedPassword() {
        RegisterRequest req = new RegisterRequest("alice", "alice@test.com", "password123");
        authService.register(req);
        User user = userRepository.findByUsername("alice").orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("password123");
        assertThat(user.getPasswordHash()).startsWith("$2a$");
    }
}
```

- [ ] **步骤 2：运行测试 — 预期 FAIL**

- [ ] **步骤 3：实现 User 实体、Repository、AuthService**

```java
// easy-wiki-server/src/main/java/com/easywiki/entity/User.java
package com.easywiki.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "avatar_url")
    private String avatarUrl;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

```java
// easy-wiki-server/src/main/java/com/easywiki/repository/UserRepository.java
package com.easywiki.repository;

import com.easywiki.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

```java
// easy-wiki-server/src/main/java/com/easywiki/dto/request/RegisterRequest.java
package com.easywiki.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Size(min = 2, max = 50) private String username;
    @NotBlank @Email private String email;
    @NotBlank @Size(min = 6, max = 100) private String password;
}
```

```java
// easy-wiki-server/src/main/java/com/easywiki/dto/request/LoginRequest.java
package com.easywiki.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank private String username;
    @NotBlank private String password;
}
```

```java
// easy-wiki-server/src/main/java/com/easywiki/service/AuthService.java
package com.easywiki.service;

import com.easywiki.dto.request.LoginRequest;
import com.easywiki.dto.request.RegisterRequest;
import com.easywiki.dto.response.AuthResponse;
import com.easywiki.entity.User;
import com.easywiki.exception.BusinessException;
import com.easywiki.repository.UserRepository;
import com.easywiki.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public void register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException(400, "邮箱已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername());
    }
}
```

> **说明：** `JwtTokenProvider` 和 `PasswordEncoder` Bean 在 Task 4 中创建。若在 Task 4 之前运行 Task 3 测试，需先实现 Task 4，或临时添加最小桩实现。

- [ ] **步骤 4：运行测试 — 预期 PASS**

- [ ] **步骤 5：提交代码**

```bash
git commit -m "feat(server): add User entity and AuthService register/login"
```

---

### Task 4：JWT 安全配置

**涉及文件：**
- 新建：`easy-wiki-server/src/main/java/com/easywiki/security/JwtTokenProvider.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/security/JwtAuthenticationFilter.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/security/UserPrincipal.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/config/SecurityConfig.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/dto/response/AuthResponse.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/controller/AuthController.java`
- 测试：`easy-wiki-server/src/test/java/com/easywiki/controller/AuthControllerTest.java`

- [ ] **步骤 1：编写会失败的登录集成测试**

```java
// easy-wiki-server/src/test/java/com/easywiki/controller/AuthControllerTest.java
package com.easywiki.controller;

import com.easywiki.dto.request.RegisterRequest;
import com.easywiki.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuthService authService;

    @BeforeEach
    void setup() {
        authService.register(new RegisterRequest("bob", "bob@test.com", "password123"));
    }

    @Test
    void loginReturnsJwt() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", "bob", "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }
}
```

- [ ] **步骤 2：运行测试 — 预期 FAIL**

- [ ] **步骤 3：实现 JWT + SecurityConfig + AuthController**

```java
// easy-wiki-server/src/main/java/com/easywiki/security/JwtTokenProvider.java
package com.easywiki.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(@Value("${easywiki.jwt.secret}") String secret,
                            @Value("${easywiki.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String createToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Long getUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return Long.parseLong(claims.getSubject());
    }
}
```

```java
// easy-wiki-server/src/main/java/com/easywiki/security/JwtAuthenticationFilter.java
package com.easywiki.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Long userId = jwtTokenProvider.getUserId(token);
                var auth = new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(userId, null), null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
```

```java
// easy-wiki-server/src/main/java/com/easywiki/security/UserPrincipal.java
package com.easywiki.security;

public record UserPrincipal(Long userId, String username) {}
```

```java
// easy-wiki-server/src/main/java/com/easywiki/config/SecurityConfig.java
package com.easywiki.config;

import com.easywiki.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/api/v1/health", "/actuator/health", "/ws/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
```

```java
// easy-wiki-server/src/main/java/com/easywiki/dto/response/AuthResponse.java
package com.easywiki.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String username;
}
```

```java
// easy-wiki-server/src/main/java/com/easywiki/controller/AuthController.java
package com.easywiki.controller;

import com.easywiki.dto.common.ApiResponse;
import com.easywiki.dto.request.LoginRequest;
import com.easywiki.dto.request.RegisterRequest;
import com.easywiki.dto.response.AuthResponse;
import com.easywiki.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ApiResponse.ok(null);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }
}
```

- [ ] **步骤 4：运行测试 — 预期 PASS**

- [ ] **步骤 5：提交代码**

```bash
git commit -m "feat(server): add JWT authentication and auth endpoints"
```

---

### Task 5：小组实体与 GroupService

**涉及文件：**
- 新建：`easy-wiki-server/src/main/java/com/easywiki/entity/Group.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/entity/GroupMember.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/entity/GroupJoinRequest.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/entity/GroupInvite.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/enums/MemberRole.java`
- 新建：`easy-wiki-server/src/main/java/com/easywiki/enums/JoinRequestStatus.java`
- 新建：相关 Repository 与 `GroupService.java`
- 测试：`easy-wiki-server/src/test/java/com/easywiki/service/GroupServiceTest.java`

- [ ] **步骤 1：编写会失败的测试 — 创建小组后创建者成为 ADMIN 成员**

```java
@SpringBootTest @ActiveProfiles("test") @Transactional
class GroupServiceTest {
    @Autowired GroupService groupService;
    @Autowired GroupMemberRepository memberRepository;
    @Autowired AuthService authService;

    @BeforeEach void setup() {
        authService.register(new RegisterRequest("admin1", "a@test.com", "pass12345"));
    }

    @Test
    void createGroupAddsAdminMember() {
        Long userId = 1L; // 测试库中第一个用户
        var group = groupService.createGroup(userId, "研发团队", "desc", true);
        var member = memberRepository.findByGroupIdAndUserId(group.getId(), userId).orElseThrow();
        assertThat(member.getRole()).isEqualTo(MemberRole.ADMIN);
    }
}
```

- [ ] **步骤 2–5：实现枚举、实体、Repository、GroupService**
  - 枚举：`MemberRole { ADMIN, MEMBER }`、`JoinRequestStatus { PENDING, APPROVED, REJECTED, EXPIRED }`
  - `GroupService` 方法：
    - `createGroup(userId, name, desc, discoverable)`
    - `listMyGroups(userId)`
    - `createInvite(groupId, adminUserId)` → UUID token，7 天过期
    - `joinByInvite(token, userId)`
    - `applyToJoin(groupId, userId, reason)`
    - `approveJoinRequest(requestId, adminUserId)` / `rejectJoinRequest(...)`
    - `isMember(groupId, userId)` — 供其他服务校验
    - `removeMember`、`leaveGroup`、`dissolveGroup`

- [ ] **步骤 6：运行测试 — 预期 PASS**

- [ ] **步骤 7：提交代码**

```bash
git commit -m "feat(server): add group management domain"
```

---

### Task 6：小组 REST API + 成员资格校验

**涉及文件：**
- 新建：`GroupController.java`、`GroupMembershipService.java`
- 测试：`GroupControllerTest.java`

- [ ] **步骤 1：编写会失败的测试 — 非成员访问小组资源返回 403**

- [ ] **步骤 2–4：实现 GroupController**（按设计文档 §6.2）；实现 `GroupMembershipService.requireMember(groupId, userId)`，非成员抛出 `BusinessException(403, ...)`

- [ ] **步骤 5：提交代码**

```bash
git commit -m "feat(server): add group REST API with membership guard"
```

---

### Task 7：Docker Compose 与 README

**涉及文件：**
- 新建：`docker-compose.yml`、`easy-wiki-server/Dockerfile`、`README.md`

- [ ] **步骤 1：创建 docker-compose.yml**

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: easywiki
      MYSQL_USER: easywiki
      MYSQL_PASSWORD: easywiki
    ports: ["3306:3306"]
    volumes: [mysql-data:/var/lib/mysql]
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
  app:
    build: ./easy-wiki-server
    ports: ["8080:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/easywiki?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: easywiki
      SPRING_DATASOURCE_PASSWORD: easywiki
      JWT_SECRET: ${JWT_SECRET:-dev-secret-change-in-production-min-32-chars!!}
      UPLOAD_PATH: /data/uploads
      DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY:-}
      AGENT_ENABLED: ${AGENT_ENABLED:-false}
    volumes: [upload-data:/data/uploads]
    depends_on:
      mysql: { condition: service_healthy }
volumes:
  mysql-data:
  upload-data:
```

- [ ] **步骤 2：验证 Docker Compose 构建**

运行：`docker compose up --build -d && curl http://localhost:8080/api/v1/health`
预期：`{"code":0,"message":"ok","data":{"status":"UP"}}`

- [ ] **步骤 3：编写 README**（环境变量、开发上传路径 `D:/easy-wiki/uploads/`）

- [ ] **步骤 4：提交代码**

```bash
git commit -m "chore: add docker-compose and setup README"
```

---

### Task 8：Android 工程脚手架 + 登录流程

**涉及文件：**
- 新建：`easy-wiki-android/`（Android Studio Empty Compose Activity）
- 新建：`SettingsDataStore.kt`、`ApiClient.kt`、`LoginScreen.kt`
- 测试：`AuthViewModelTest.kt`

- [ ] **步骤 1：创建 Android 工程** — Android Studio → Empty Compose Activity，包名 `com.easywiki`，minSdk 26，Kotlin

- [ ] **步骤 2：在 `app/build.gradle.kts` 添加 Retrofit + DataStore 依赖**

- [ ] **步骤 3：编写会失败的 AuthViewModelTest** — 登录成功后 Token 状态更新

- [ ] **步骤 4：实现 SettingsDataStore（serverUrl、jwtToken）、ApiClient、AuthViewModel、LoginScreen**

- [ ] **步骤 5：首次启动增加服务端地址配置页**（DataStore `server_url` 默认为空）

- [ ] **步骤 6：运行单元测试 — 预期 PASS**

- [ ] **步骤 7：手动测试** — 对接本地后端登录

- [ ] **步骤 8：提交代码**

```bash
git commit -m "feat(android): scaffold Compose app with login flow"
```

**P1 里程碑：** 用户可通过 API 注册/登录；创建/列出小组；Docker 后端可运行；Android 可完成登录。

---

## 阶段 P2：核心协作（Wiki + 任务）

### Task 9：Wiki 实体、服务、乐观锁

**涉及文件：**
- 新建：`WikiPage.java`、`WikiPageRepository.java`、`WikiService.java`、`WikiController.java`
- 测试：`WikiServiceTest.java`

- [ ] **步骤 1：编写会失败的测试 — 过期 version 更新抛出 ConflictException**

```java
@Test
void updateWithStaleVersionThrowsConflict() {
    WikiPage page = wikiService.createPage(groupId, userId, null, "标题", "# content");
    wikiService.updatePage(groupId, userId, page.getId(), "新标题", "# new", page.getVersion());
    assertThatThrownBy(() -> wikiService.updatePage(groupId, userId, page.getId(),
            "x", "y", page.getVersion()))
            .isInstanceOf(ConflictException.class);
}
```

- [ ] **步骤 2：实现 WikiPage**（`@Version private Integer version`、树结构 `parentId`、`sortOrder`）

- [ ] **步骤 3：实现 WikiService** — `getTree`、`createPage`、`getPage`、`updatePage`（乐观锁）、`deletePage`（有子节点则禁止）、`search(keyword)`

- [ ] **步骤 4：实现 WikiController**（路径 `/api/v1/groups/{groupId}/wiki/*`）

- [ ] **步骤 5：运行测试 — 预期 PASS；提交**

```bash
git commit -m "feat(server): add wiki with optimistic locking"
```

---

### Task 10：Wiki 图片本地上传

**涉及文件：**
- 新建：`FileService.java`、`WebMvcConfig.java`
- 修改：`WikiController.java`
- 测试：`FileServiceTest.java`

- [ ] **步骤 1：编写会失败的测试 — 保存文件到临时目录并返回 URL**

- [ ] **步骤 2：实现 FileService** — 校验扩展名（jpg/png/gif/webp）、最大 5MB，路径 `{uploadPath}/{groupId}/{uuid}.ext`

- [ ] **步骤 3：添加 `POST /wiki/upload`  multipart 端点**

- [ ] **步骤 4：注册静态资源处理器 `/uploads/**` → `file:///D:/easy-wiki/uploads/`**

- [ ] **步骤 5：提交**

```bash
git commit -m "feat(server): add local disk wiki image upload"
```

---

### Task 11：Task 实体与指派状态机

**涉及文件：**
- 新建：`Task.java`、`TaskLog.java`、状态/优先级/指派枚举、`TaskService.java`、`TaskController.java`
- 测试：`TaskServiceTest.java`

- [ ] **步骤 1：编写会失败的状态机测试：**
  - 指派 → `PENDING_ACCEPT`
  - 确认接取 → `ACCEPTED`
  - 拒绝 → `UNASSIGNED`
  - 主动接取未指派任务 → `ACCEPTED`
  - 转派 → 新执行人 `PENDING_ACCEPT`

- [ ] **步骤 2：实现 TaskService** — `createTask`、`updateStatus`、`assign`、`accept`、`reject`、`claim`、`transfer`、`listByGroup`

- [ ] **步骤 3：实现 TaskController**（按设计文档 §6.4）

- [ ] **步骤 4：运行测试 — 预期 PASS；提交**

```bash
git commit -m "feat(server): add task board with assignment state machine"
```

---

### Task 12：Android Wiki 页面

**涉及文件：**
- 新建：`WikiTreeScreen.kt`、`WikiDetailScreen.kt`、`WikiViewModel.kt`、`WikiRepository.kt`

- [ ] **步骤 1：在 Retrofit 接口中添加 Wiki API 方法**

- [ ] **步骤 2：WikiTreeScreen** — 懒加载目录树列表，点击跳转详情

- [ ] **步骤 3：WikiDetailScreen** — Markwon 渲染阅读；编辑模式 TextField + 保存（携带 version）

- [ ] **步骤 4：处理 409 冲突** — Snackbar 提示「页面已被他人更新，请刷新」

- [ ] **步骤 5：手动 E2E 测试；提交**

```bash
git commit -m "feat(android): add wiki tree and detail screens"
```

---

### Task 13：Android 任务看板页面

**涉及文件：**
- 新建：`TaskBoardScreen.kt`、`TaskDetailScreen.kt`、`TaskViewModel.kt`

- [ ] **步骤 1：TaskBoardScreen** — 3 个 Tab（TODO/IN_PROGRESS/DONE），卡片按优先级着色

- [ ] **步骤 2：TaskDetailScreen** — 指派、确认接取、拒绝、主动接取操作

- [ ] **步骤 3：新建任务 FAB**

- [ ] **步骤 4：手动 E2E 测试；提交**

```bash
git commit -m "feat(android): add task board and detail screens"
```

**P2 里程碑：** Wiki CRUD + 搜索 + 图片上传；任务看板 + 指派流程端到端可用。

---

## 阶段 P3：实时能力（WebSocket 聊天 + 通知 + FCM）

### Task 14：通知实体与 NotificationService

**涉及文件：**
- 新建：`Notification.java`、`UserDevice.java`、`NotificationEventType.java`、`NotificationService.java`、相关 Controller
- 测试：`NotificationServiceTest.java`

- [ ] **步骤 1：编写会失败的测试 — publish 创建数据库记录**

- [ ] **步骤 2：实现 `NotificationService.publish(NotificationEvent)`** — 持久化，再调用 WsSessionManager + FcmPushService

- [ ] **步骤 3：在 GroupService（入组申请）、TaskService（指派/接取）、WikiService（更新）中接入通知**

- [ ] **步骤 4：REST 接口** — `GET /api/v1/notifications`、`PUT /api/v1/notifications/{id}/read`、`POST /api/v1/devices`

- [ ] **步骤 5：提交**

```bash
git commit -m "feat(server): add notification service and REST API"
```

---

### Task 15：WebSocket 基础设施

**涉及文件：**
- 新建：`WebSocketConfig.java`、`WsAuthInterceptor.java`、`WsSessionManager.java`、`WsMessageHandler.java`、`WsMessage.java`
- 测试：`WsSessionManagerTest.java`

- [ ] **步骤 1：编写会失败的测试 — 按 userId 注册会话，广播可送达**

- [ ] **步骤 2：配置 `/ws` 端点**，Query 参数 `?token=` JWT 鉴权

- [ ] **步骤 3：处理消息类型** — `PING→PONG`、`CHAT_MESSAGE`、向用户会话推送 `NOTIFICATION`

- [ ] **步骤 4：提交**

```bash
git commit -m "feat(server): add WebSocket session management"
```

---

### Task 16：群聊服务与 WebSocket 集成

**涉及文件：**
- 新建：`ChatMessage.java`、`ChatService.java`、`ChatController.java`
- 测试：`ChatServiceTest.java`

- [ ] **步骤 1：编写会失败的测试 — 发送消息持久化并解析 @mentions**

- [ ] **步骤 2：ChatService.sendMessage** — 校验 ≤2000 字符，保存，WebSocket 广播，触发 @ 通知

- [ ] **步骤 3：`GET /api/v1/groups/{gid}/chat/messages?page&size`** — 默认 50 条，按 sentAt 降序

- [ ] **步骤 4：提交**

```bash
git commit -m "feat(server): add group chat with WebSocket broadcast"
```

---

### Task 17：FCM 服务端推送

**涉及文件：**
- 新建：`FcmPushService.java`、`FirebaseConfig.java`
- 修改：`pom.xml`（添加 firebase-admin）

- [ ] **步骤 1：在 pom.xml 添加 firebase-admin 依赖**

- [ ] **步骤 2：FirebaseConfig** — 从 `FCM_CREDENTIALS` 路径初始化；缺失时优雅跳过

- [ ] **步骤 3：FcmPushService.send(userId, title, body, data)** — 查找 UserDevice Token 并推送

- [ ] **步骤 4：NotificationService 在无活跃 WS 连接时（或始终后台场景）调用 FcmPushService**

- [ ] **步骤 5：提交**

```bash
git commit -m "feat(server): add FCM push integration"
```

---

### Task 18：Android WebSocket + 聊天 + 通知 + FCM

**涉及文件：**
- 新建：`WebSocketManager.kt`、`ChatScreen.kt`、`NotificationScreen.kt`、`FcmService.kt`

- [ ] **步骤 1：WebSocketManager** — 进入工作台时连接，30 秒 PING，指数退避重连

- [ ] **步骤 2：ChatScreen** — LazyColumn 消息列表、输入框、@ 成员选择器

- [ ] **步骤 3：NotificationScreen** — 列表、标已读、Deep Link 跳转

- [ ] **步骤 4：集成 Firebase Messaging**，通过 `POST /devices` 注册 Token

- [ ] **步骤 5：FcmService** — 处理通知点击 → NavController 路由跳转

- [ ] **步骤 6：提交**

```bash
git commit -m "feat(android): add chat, notifications, and FCM"
```

**P3 里程碑：** 群聊实时可用；通知通过 WebSocket + FCM 双通道送达。

---

## 阶段 P4：Agent 助手（DeepSeek）

### Task 19：DeepSeek 客户端与 AgentService

**涉及文件：**
- 新建：`DeepSeekClient.java`、`PromptBuilder.java`、`AgentIntent.java`、`AgentService.java`、`AgentController.java`
- 测试：`PromptBuilderTest.java`、`AgentServiceTest.java`（Mock DeepSeekClient）

- [ ] **步骤 1：编写会失败的 PromptBuilderTest** — 任务列表注入 Prompt**

- [ ] **步骤 2：实现 DeepSeekClient** — OkHttp POST `{baseUrl}/v1/chat/completions`，OpenAI 格式

```java
// 请求体结构
// { "model": "deepseek-chat", "messages": [{"role":"system","content":"..."},{"role":"user","content":"..."}], "max_tokens": 8192 }
```

- [ ] **步骤 3：AgentService.chat(groupId, userId, message, sessionHistory)** — 关键词识别意图，加载 Wiki/任务上下文，调用 DeepSeek，返回 Markdown 回复

- [ ] **步骤 4：接口** — `POST /api/v1/groups/{gid}/agent/chat`、`POST .../agent/tasks/create`（解析 JSON 任务建议）

- [ ] **步骤 5：`agent.enabled=false` 时返回 503 及提示信息**

- [ ] **步骤 6：提交**

```bash
git commit -m "feat(server): add DeepSeek agent assistant"
```

---

### Task 20：Android Agent 页面

**涉及文件：**
- 新建：`AgentScreen.kt`、`AgentViewModel.kt`

- [ ] **步骤 1：对话式 UI** — 消息气泡，助手回复 Markdown 渲染

- [ ] **步骤 2：ViewModel 保留最近 10 轮对话上下文**

- [ ] **步骤 3：任务建议展示「采纳」按钮** → 调用批量创建 API

- [ ] **步骤 4：提交**

```bash
git commit -m "feat(android): add agent assistant screen"
```

**P4 里程碑：** Agent 可摘要 Wiki、整理任务、建议并创建任务。

---

## 阶段 P5：定时任务、打磨、发布

### Task 21：定时调度任务

**涉及文件：**
- 新建：`TaskReminderScheduler.java`、`JoinRequestExpiryScheduler.java`
- 测试：`TaskReminderSchedulerTest.java`

- [ ] **步骤 1：TaskReminderScheduler** — 每小时 cron，查找 24h 内到期及当天 9:00（Asia/Shanghai）到期任务，发送通知

- [ ] **步骤 2：指派超时** — `PENDING_ACCEPT` 超过 7 天 → 重置为 `UNASSIGNED`，通知创建人

- [ ] **步骤 3：JoinRequestExpiryScheduler** — PENDING 超过 30 天 → EXPIRED

- [ ] **步骤 4：提交**

```bash
git commit -m "feat(server): add scheduled reminders and expiry jobs"
```

---

### Task 22：Android 工作台导航壳

**涉及文件：**
- 新建：`WorkspaceScreen.kt`、`NavGraph.kt`、`GroupListScreen.kt`

- [ ] **步骤 1：GroupListScreen** — 我的小组、创建小组、发现/搜索、申请加入

- [ ] **步骤 2：WorkspaceScreen** — 底部导航：Wiki / 看板 / 聊天 / 通知 / Agent

- [ ] **步骤 3：管理员页面** — 入组申请审批、邀请链接生成、成员列表

- [ ] **步骤 4：提交**

```bash
git commit -m "feat(android): add workspace navigation and group management UI"
```

---

### Task 23：端到端验收清单

- [ ] **步骤 1：运行完整后端测试套件**

运行：`cd easy-wiki-server && mvn test`
预期：全部 PASS

- [ ] **步骤 2：Docker Compose 冒烟测试**

运行：`docker compose up --build -d && curl http://localhost:8080/api/v1/health`

- [ ] **步骤 3：手动 E2E 脚本**（写入 README）：
  1. 注册用户 A、B
  2. A 创建小组并邀请 B
  3. A 创建 Wiki 页面，B 收到通知
  4. A 创建任务并指派 B，B 确认接取
  5. 群聊发送 @ 消息
  6. Agent 摘要 Wiki（需配置 DEEPSEEK_API_KEY）
  7. App 切后台，触发 FCM 推送

- [ ] **步骤 4：构建 Release APK**

运行：`cd easy-wiki-android && ./gradlew assembleRelease`

- [ ] **步骤 5：更新 README** — 部署说明、环境变量、Firebase 配置、APK 安装

- [ ] **步骤 6：提交**

```bash
git commit -m "docs: add E2E verification checklist and deployment guide"
```

**P5 里程碑：** V1.0 MVP 可用于内部发布。

---

## 设计文档覆盖检查

| 设计/需求项 | 对应 Task |
|-------------|-----------|
| 用户注册/登录 JWT | Task 3, 4, 8 |
| 小组创建/邀请/申请/审批 | Task 5, 6, 22 |
| 组间数据隔离 | Task 5, 6（成员资格校验） |
| Wiki Markdown/目录/搜索 | Task 9, 12 |
| Wiki 乐观锁 | Task 9 |
| Wiki 图片上传（本地磁盘） | Task 10 |
| 任务看板 CRUD/状态/优先级 | Task 11, 13 |
| 指派确认接取状态机 | Task 11 |
| 通知持久化 + WebSocket + FCM | Task 14, 15, 17, 18 |
| 群聊 + @提及 | Task 16, 18 |
| Agent DeepSeek | Task 19, 20 |
| 截止提醒/指派超时/申请过期 | Task 21 |
| Docker 部署 | Task 7 |
| Android API 26+ | Task 8 |
| 上传路径 D:/easy-wiki/uploads/ | Task 10, application.yml |

**遗漏项：** 无 — V1.0 全部需求已映射。

---

## 并行开发策略

| 轨道 | 负责范围 | 启动时机 |
|------|----------|----------|
| 后端 | Task 1–7, 9–11, 14–17, 19, 21 | 立即开始 |
| Android | Task 8, 12–13, 18, 20, 22 | Task 4 完成后（API 稳定）；各模块完成前可用 MockWebServer |

Android 开发者可在后端各模块 API 就绪前，使用 Mock 响应并行开发。
