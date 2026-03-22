# SmartHire

SmartHire 是一个围绕 Java、Spring Boot、Spring Security、JWT、MySQL 等技术栈搭建的招聘管理练手项目，目标是把认证鉴权、业务建模、缓存、消息队列、定时任务、数据库迁移、容器化和前后端协作串成一条完整的练习链路。

## 项目定位

- 做一个可以写进简历、也能在面试里讲清楚的 Java 项目
- 优先完成真实业务闭环，而不是堆很多零散功能
- 优先保证后端完整性，再补前端展示和工程增强
- 强调业务建模、权限设计、状态流转和工程化能力

## 成功标准

- 候选人可以注册、登录、浏览岗位、投递岗位、查看投递记录
- HR 可以发布岗位、管理岗位、查看投递列表、推进申请状态、安排面试
- 系统支持 JWT 鉴权与基础 RBAC 权限控制
- 系统支持通知记录，形成完整招聘流程闭环
- 仓库包含 README、数据库迁移脚本、接口说明，并能口头讲清楚核心业务与工程设计

## 技术栈

- 后端：Java 21、Spring Boot 3.3.4、Spring Security、MyBatis-Plus、JWT、Flyway、Maven
- 数据与中间件：MySQL 8、Redis、RabbitMQ
- 前端：React 18、TypeScript、Vite、React Router、TanStack Query
- 工程化：Docker、Docker Compose、Springdoc OpenAPI / Swagger UI、GitHub Actions
- 测试：JUnit 5、MockMvc、Mockito、Testcontainers

## 范围拆分

### P0：核心闭环

- 注册、登录、密码加密、JWT 鉴权、基础 RBAC
- 岗位创建、更新、删除、详情、分页与条件搜索
- 候选人投递岗位、查看个人投递、HR 查看岗位投递
- 防重复投递与申请状态流转
- HR 安排面试、候选人查看面试、更新面试结果
- 投递成功、状态更新、面试安排等站内通知

### P1：完整度增强

- 简历 PDF 上传
- 基础数据统计
- 管理员轻量后台
- README 与接口文档完善
- Docker 化部署
- 基础操作日志

### P2：工程亮点

- Redis 缓存岗位列表、岗位详情与统计概览
- RabbitMQ 异步通知链路
- 定时任务关闭过期岗位与发送面试提醒
- 更细的统计图表与后台可视化
- GitHub Actions CI/CD 与部署校验

### 当前明确不做

- 复杂动态角色权限管理平台
- 多轮面试流程编排
- 邮件 / 短信真实发送
- 大而全的后台管理系统
- 过度复杂的前端 UI

## 目前已完成的核心流程

- 认证与权限：支持 `CANDIDATE / HR` 自助注册、JWT 登录、当前用户信息查询，`ADMIN` 账号单独维护
- 岗位流程：支持岗位创建、修改、删除、详情与分页筛选，HR 只能管理自己发布的岗位，管理员可跨岗位查看与管理
- 投递流程：候选人可上传 PDF 简历、投递岗位、查看自己的投递记录，同一候选人对同一岗位防重复投递
- 招聘流转：HR / Admin 可查看投递、推进状态、安排或更新面试，申请状态会联动进入面试阶段
- 通知流程：投递、状态更新、面试安排、面试更新、面试提醒都会通过 RabbitMQ 异步生成站内通知
- 统计与后台：支持 HR 视角和 Admin 视角的统计概览，管理员可管理用户、查看岗位总览与操作日志
- 工程增强：已接入 Redis 缓存、Flyway 数据库迁移、岗位过期定时任务、面试提醒定时任务、Docker Compose、本地独立前端和基础 CI/CD 骨架

当前主链路已经打通：

`auth -> jobs -> applications -> interviews -> notifications`

## 接下来要做的流程

- 继续完善 HR / Admin 的统计图表与后台可视化体验
- 补更完整的日报类或汇总类定时任务
- 继续增强部署链路，包括真实环境接入、域名 / HTTPS、回滚策略
- 视情况扩展通知消费者，例如邮件或短信提醒

## 仓库结构

```text
SmartHire/
├── backend/                     # Spring Boot 后端
├── frontend/                    # 独立前端应用
├── docs/                        # 演示说明与补充资料
├── sql/                         # 数据库初始化与种子数据脚本
└── docker-compose.yml
```

## 快速开始

### 推荐方式：Docker Compose 一键启动

如果你本地已经安装 Docker，直接在项目根目录执行：

```bash
docker compose up --build
```

这会一次性拉起：

- MySQL
- Redis
- RabbitMQ
- Spring Boot backend
- 独立 React frontend

backend 启动后会自动通过 Flyway 执行 `backend/src/main/resources/db/migration` 下的结构迁移脚本。

如果你想导入演示数据，等服务启动成功后执行：

```bash
docker compose exec -T mysql mysql -uroot -proot smarthire < sql/002_seed_demo_data.sql
```

默认访问地址：

- `http://localhost:5173` - Independent React frontend
- `http://localhost:8080` - SmartHire backend
- `http://localhost:8080/swagger-ui.html` - Swagger UI
- `http://localhost:15672` - RabbitMQ Management UI

如果你之前已经启动过旧版本的 MySQL volume，最简单的本地升级方式是：

- 运行 `docker compose down -v`
- 然后重新执行 `docker compose up --build`

停止服务：

```bash
docker compose down
```

如果想连同数据库卷一起删除：

```bash
docker compose down -v
```

### 可选：本地拆分启动

如果你想本地拆开跑，也可以：

- backend：`cd backend && mvn spring-boot:run`
- frontend：`cd frontend && npm install && npm run dev`
- backend 地址：`http://localhost:8080`
- frontend 地址：`http://localhost:5173`
- Swagger：`http://localhost:8080/swagger-ui.html`

环境变量和更多部署细节可以再看：

- `frontend/.env.example`
- `backend/src/main/resources/application.yml`
- `deploy/README.md`

简单验证：

- backend 测试：`cd backend && mvn test`
- 当前后端测试结果：`93` 个测试通过

仓库里也已经有基础的 GitHub Actions CI/CD 骨架，用于测试、构建镜像和部署流程练习。

## 角色与账号说明

- `CANDIDATE`：浏览岗位、上传简历、投递岗位、查看申请/面试/通知
- `HR`：管理自己创建的岗位、查看投递、推进状态、安排和维护面试
- `ADMIN`：查看全局统计、跨岗位管理、管理用户状态、查看操作日志

账号方面：

- 注册接口支持自助创建 `CANDIDATE / HR`
- `ADMIN` 建议通过演示数据或手动绑定角色方式准备
- 如果导入了 `sql/002_seed_demo_data.sql`，可直接使用仓库里的 demo 账号

## 核心接口总览

| 模块 | 方法 | 路径 | 角色 |
| --- | --- | --- | --- |
| 系统 | `GET` | `/api/health` | 公开 |
| 认证 | `POST` | `/api/auth/register` | 公开 |
| 认证 | `POST` | `/api/auth/login` | 公开 |
| 认证 | `GET` | `/api/auth/me` | 已登录 |
| 简历 | `POST` | `/api/resumes/upload` | `CANDIDATE` |
| 岗位 | `GET` | `/api/jobs` | 公开 |
| 岗位 | `GET` | `/api/jobs/{id}` | 公开 |
| 岗位 | `POST` | `/api/jobs` | `HR/ADMIN` |
| 岗位 | `PUT` | `/api/jobs/{id}` | `HR/ADMIN` |
| 岗位 | `DELETE` | `/api/jobs/{id}` | `HR/ADMIN` |
| 统计 | `GET` | `/api/statistics/overview` | `HR/ADMIN` |
| 管理员 | `GET` | `/api/admin/users` | `ADMIN` |
| 管理员 | `PATCH` | `/api/admin/users/{id}/status` | `ADMIN` |
| 日志 | `GET` | `/api/admin/operation-logs` | `ADMIN` |
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
  "phone": "13800138000",
  "roleCode": "CANDIDATE"
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
  "resumeFilePath": "resumes/1/resume-abc123.pdf",
  "coverLetter": "I am very interested in this role."
}
```

### 上传简历

```bash
curl -X POST http://localhost:8080/api/resumes/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@./resume.pdf;type=application/pdf"
```

### 查看统计概览

```bash
curl http://localhost:8080/api/statistics/overview \
  -H "Authorization: Bearer <token>"
```

### 查看用户列表

```bash
curl "http://localhost:8080/api/admin/users?page=1&size=10&status=ACTIVE&roleCode=HR" \
  -H "Authorization: Bearer <admin-token>"
```

### 禁用用户

```bash
curl -X PATCH http://localhost:8080/api/admin/users/2/status \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"status":"DISABLED"}'
```

### 查看操作日志

```bash
curl "http://localhost:8080/api/admin/operation-logs?page=1&size=10&targetType=USER" \
  -H "Authorization: Bearer <admin-token>"
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

## 补充说明

- 数据库结构迁移由 Flyway 管理，迁移脚本在 [db/migration](/Users/jay/Projects/SmartHire/backend/src/main/resources/db/migration)
- 更多补充资料可以看 [demo-guide.md](/Users/jay/Projects/SmartHire/docs/demo-guide.md) 和 [interview-questions.md](/Users/jay/Projects/SmartHire/docs/interview-questions.md)
