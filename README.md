# SmartHire

SmartHire 是一个面向校招/招聘场景的招聘平台后端练手项目，目标是做出一条能写进简历、也能在面试里讲清楚的业务闭环。

当前仓库已经完成后端核心流程：

- 候选人注册、登录、查看个人信息
- HR/管理员发布岗位、修改岗位、删除岗位
- 候选人查看岗位、投递岗位、防重复投递
- HR/管理员查看投递并更新状态
- HR/管理员安排面试、更新面试
- 站内通知联动投递和面试流程

## 当前阶段

项目目前处于 `P0 核心业务闭环已完成` 阶段，后端主链路已经打通：

`auth -> jobs -> applications -> interviews -> notifications`

已完成但还可以继续增强的方向：

- 简历文件上传
- 统计面板和后台增强
- Postman 集合或前端联调
- 真实前端页面与联调截图

## 技术栈

- Java 21
- Spring Boot 3.3.4
- Spring Security
- MyBatis-Plus
- MySQL 8
- JWT
- Springdoc OpenAPI / Swagger UI
- Maven
- JUnit 5 + MockMvc + Mockito

## 已实现模块

### 1. 认证与权限

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`

说明：

- 注册接口默认创建 `CANDIDATE` 角色账号
- 鉴权方式为 JWT Bearer Token
- 当前角色模型为 `CANDIDATE / HR / ADMIN`

### 2. 岗位模块

- `POST /api/jobs`
- `PUT /api/jobs/{id}`
- `DELETE /api/jobs/{id}`
- `GET /api/jobs/{id}`
- `GET /api/jobs`

支持能力：

- 岗位创建、更新、删除、详情
- 关键词、城市、分类、状态筛选
- 分页查询
- HR 只能管理自己发布的岗位，管理员可管理全部岗位

### 3. 投递模块

- `POST /api/applications`
- `GET /api/applications/me`
- `GET /api/jobs/{jobId}/applications`
- `PATCH /api/applications/{id}/status`

支持能力：

- 候选人投递岗位
- 同一候选人同一岗位防重复投递
- HR 查看自己岗位下的投递记录
- 投递状态流转：`APPLIED / REVIEWING / INTERVIEW / OFFERED / REJECTED`

### 4. 面试模块

- `POST /api/interviews`
- `PATCH /api/interviews/{id}`
- `GET /api/interviews/me`
- `GET /api/jobs/{jobId}/interviews`

支持能力：

- 为申请安排面试
- 每个申请当前只允许一条面试记录
- 更新面试时间、地点、链接、状态、结果
- 面试安排时会自动推进申请状态到 `INTERVIEW`

### 5. 通知模块

- `GET /api/notifications`
- `GET /api/notifications/unread-count`
- `PATCH /api/notifications/{id}/read`

当前自动通知场景：

- 候选人投递成功后，通知岗位发布者
- HR 更新投递状态后，通知候选人
- HR 安排面试后，通知候选人
- HR 更新面试后，通知候选人

## 仓库结构

```text
SmartHire/
├── backend/                     # Spring Boot 后端
├── docs/                        # 演示说明与截图清单
├── sql/                         # 数据库初始化与种子数据脚本
├── docker-compose.yml
└── smarthire_development_plan.md
```

## 快速开始

### 1. 初始化数据库

先创建数据库：

```sql
CREATE DATABASE smarthire CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

再执行初始化脚本：

```bash
mysql -u root -p smarthire < sql/001_init_schema.sql
```

如果你想直接导入演示数据，再执行：

```bash
mysql -u root -p smarthire < sql/002_seed_demo_data.sql
```

`001` 会初始化：

- 核心表结构
- 角色数据：`CANDIDATE`、`HR`、`ADMIN`

`002` 会初始化：

- 演示账号
- 演示岗位
- 演示投递、面试与通知数据

### 2. 配置环境变量

本地开发建议使用 `JDK 21`。

后端支持以下环境变量：

| 变量名 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` | `localhost` | MySQL 地址 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `smarthire` | 数据库名 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `root` | 数据库密码 |
| `SERVER_PORT` | `8080` | 服务端口 |
| `JWT_SECRET` | `smarthire-dev-secret-key-at-least-32-bytes-long` | JWT 密钥 |
| `JWT_ACCESS_TOKEN_EXPIRATION_SECONDS` | `86400` | Token 过期时间，单位秒 |

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

启动后可访问健康检查：

```text
GET http://localhost:8080/api/health
```

Swagger / OpenAPI 文档地址：

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

### 4. 运行测试

```bash
cd backend
mvn test
```

当前后端测试结果：

- `40` 个测试通过
- 覆盖认证、岗位、投递、面试、通知、Swagger 文档端点和基础安全规则

### 5. 使用 Docker Compose 启动

如果你本地已经安装 Docker，也可以直接在项目根目录执行：

```bash
docker compose up --build
```

首次启动时，MySQL 会按顺序自动执行 `sql/001_init_schema.sql` 和 `sql/002_seed_demo_data.sql`。

服务默认暴露：

- `http://localhost:8080` - SmartHire backend
- `localhost:3306` - MySQL

停止服务：

```bash
docker compose down
```

如果想连同数据库卷一起删除：

```bash
docker compose down -v
```

## 角色说明

### CANDIDATE

- 注册登录
- 浏览岗位
- 投递岗位
- 查看我的投递
- 查看我的面试
- 查看和已读我的通知

### HR

- 发布和维护自己创建的岗位
- 查看自己岗位下的投递
- 更新投递状态
- 安排和维护面试

### ADMIN

- 具备 HR 能力
- 可跨岗位管理业务数据

## 如何创建 HR / ADMIN 账号

当前注册接口默认只能注册候选人账号。如果你已经执行了 `sql/002_seed_demo_data.sql`，可以直接使用下面的演示账号。

| 角色 | 邮箱 | 密码 |
| --- | --- | --- |
| Candidate | `candidate@example.com` | `password123` |
| Candidate | `candidate2@example.com` | `password123` |
| HR | `hr@example.com` | `password123` |
| Admin | `admin@example.com` | `password123` |

如果你没有导入演示数据，也可以先调用注册接口创建普通用户，再手动绑定角色。

示例：把某个用户绑定为 HR。

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'HR'
WHERE u.email = 'hr@example.com';
```

如果用户已经是候选人，也可以保留原角色，再额外赋予 `HR` 或 `ADMIN`。

## 核心接口总览

| 模块 | 方法 | 路径 | 角色 |
| --- | --- | --- | --- |
| 系统 | `GET` | `/api/health` | 公开 |
| 认证 | `POST` | `/api/auth/register` | 公开 |
| 认证 | `POST` | `/api/auth/login` | 公开 |
| 认证 | `GET` | `/api/auth/me` | 已登录 |
| 岗位 | `GET` | `/api/jobs` | 公开 |
| 岗位 | `GET` | `/api/jobs/{id}` | 公开 |
| 岗位 | `POST` | `/api/jobs` | `HR/ADMIN` |
| 岗位 | `PUT` | `/api/jobs/{id}` | `HR/ADMIN` |
| 岗位 | `DELETE` | `/api/jobs/{id}` | `HR/ADMIN` |
| 投递 | `POST` | `/api/applications` | `CANDIDATE` |
| 投递 | `GET` | `/api/applications/me` | `CANDIDATE` |
| 投递 | `GET` | `/api/jobs/{jobId}/applications` | `HR/ADMIN` |
| 投递 | `PATCH` | `/api/applications/{id}/status` | `HR/ADMIN` |
| 面试 | `POST` | `/api/interviews` | `HR/ADMIN` |
| 面试 | `PATCH` | `/api/interviews/{id}` | `HR/ADMIN` |
| 面试 | `GET` | `/api/interviews/me` | `CANDIDATE` |
| 面试 | `GET` | `/api/jobs/{jobId}/interviews` | `HR/ADMIN` |
| 通知 | `GET` | `/api/notifications` | 已登录 |
| 通知 | `GET` | `/api/notifications/unread-count` | 已登录 |
| 通知 | `PATCH` | `/api/notifications/{id}/read` | 已登录 |

## 示例请求

### 注册

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "candidate@example.com",
  "password": "password123",
  "fullName": "Candidate User",
  "phone": "13800138000"
}
```

### 登录

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "candidate@example.com",
  "password": "password123"
}
```

### 创建岗位

```http
POST /api/jobs
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Backend Engineer",
  "description": "Build SmartHire backend services",
  "city": "Shanghai",
  "category": "Engineering",
  "employmentType": "FULL_TIME",
  "experienceLevel": "JUNIOR",
  "salaryMin": 15000,
  "salaryMax": 25000,
  "status": "OPEN"
}
```

### 投递岗位

```http
POST /api/applications
Authorization: Bearer <token>
Content-Type: application/json

{
  "jobId": 1,
  "resumeFilePath": "/resume/candidate-a.pdf",
  "coverLetter": "I am very interested in this role."
}
```

### 安排面试

```http
POST /api/interviews
Authorization: Bearer <token>
Content-Type: application/json

{
  "applicationId": 1,
  "interviewAt": "2026-03-20T15:00:00",
  "location": "Meeting Room A",
  "meetingLink": "https://meet.example.com/smarthire",
  "remark": "Please prepare a short project introduction."
}
```

## 数据库说明

核心表已经落地在 [sql/001_init_schema.sql](sql/001_init_schema.sql)：

- `users`
- `roles`
- `user_roles`
- `jobs`
- `applications`
- `interviews`
- `notifications`

其中关键约束包括：

- 同一候选人同一岗位只能投递一次
- 每个申请当前只允许一条面试记录
- 通知表支持未读统计和已读状态更新

## 下一步建议

如果继续往下做，最值得补的顺序是：

1. 增加简历上传能力
2. 导出 Postman 集合或补更细的接口说明
3. 接一个轻量前端，把闭环真正跑起来
4. 继续补管理员视角和基础统计

## 演示资料

仓库里已经预留了演示说明与截图清单：

- [docs/demo-guide.md](/Users/jay/Projects/SmartHire/docs/demo-guide.md)
- [docs/screenshots/README.md](/Users/jay/Projects/SmartHire/docs/screenshots/README.md)
