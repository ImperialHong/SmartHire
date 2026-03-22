# SmartHire

SmartHire 是一个围绕 Java、Spring Boot、Spring Security、JWT、MySQL 等技术栈搭建的招聘管理练手项目，目标是把认证鉴权、业务建模、缓存、消息队列、定时任务、数据库迁移、容器化和前后端协作串成一条完整的练习链路。

## 技术栈

- 后端：Java 21、Spring Boot 3.3.4、Spring Security、MyBatis-Plus、JWT、Flyway、Maven
- 数据与中间件：MySQL 8、Redis、RabbitMQ
- 前端：React 18、TypeScript、Vite、React Router、TanStack Query
- 工程化：Docker、Docker Compose、Springdoc OpenAPI / Swagger UI、GitHub Actions
- 测试：JUnit 5、MockMvc、Mockito、Testcontainers

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
├── docker-compose.yml
└── smarthire_development_plan.md
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

本地开发建议使用 `JDK 21`。

后端支持以下环境变量：

| 变量名 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` | `localhost` | MySQL 地址 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `smarthire` | 数据库名 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `root` | 数据库密码 |
| `REDIS_HOST` | `localhost` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ 地址 |
| `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP 端口 |
| `RABBITMQ_USERNAME` | `guest` | RabbitMQ 用户名 |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ 密码 |
| `SPRING_FLYWAY_ENABLED` | `true` | 是否启用 Flyway 结构迁移 |
| `SPRING_FLYWAY_BASELINE_ON_MIGRATE` | `false` | 是否在已有非空库上做 Flyway baseline |
| `SPRING_FLYWAY_BASELINE_VERSION` | `3` | baseline 时接管的当前结构版本 |
| `APP_JOBS_EXPIRATION_ENABLED` | `true` | 是否开启岗位过期定时任务 |
| `APP_JOBS_EXPIRATION_CRON` | `0 */5 * * * *` | 过期岗位扫描 cron |
| `APP_JOBS_EXPIRATION_ZONE` | `Asia/Shanghai` | 定时任务时区 |
| `APP_INTERVIEWS_REMINDER_ENABLED` | `true` | 是否开启面试提醒定时任务 |
| `APP_INTERVIEWS_REMINDER_CRON` | `0 */10 * * * *` | 面试提醒扫描 cron |
| `APP_INTERVIEWS_REMINDER_ZONE` | `Asia/Shanghai` | 面试提醒定时任务时区 |
| `APP_INTERVIEWS_REMINDER_UPCOMING_WINDOW_HOURS` | `24` | 未来多少小时内发送常规面试提醒 |
| `APP_INTERVIEWS_REMINDER_STARTING_SOON_WINDOW_MINUTES` | `60` | 未来多少分钟内发送即将开始提醒 |
| `SPRING_CACHE_TYPE` | `redis` | Spring Cache 类型 |
| `SERVER_PORT` | `8080` | 服务端口 |
| `JWT_SECRET` | `smarthire-dev-secret-key-at-least-32-bytes-long` | JWT 密钥 |
| `JWT_ACCESS_TOKEN_EXPIRATION_SECONDS` | `86400` | Token 过期时间，单位秒 |
| `RESUME_STORAGE_DIR` | `uploads/resumes` | 简历本地存储目录 |
| `RESUME_MAX_SIZE_BYTES` | `5242880` | 简历文件大小上限，默认 5 MB |
| `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` | `5MB` | 单文件上传限制 |
| `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE` | `5MB` | 单请求上传限制 |

独立前端参考环境变量见：

- `frontend/.env.example`

默认开发约定：

- 前端 `5173`
- 后端 `8080`
- Vite 本地代理 `/api -> http://localhost:8080`

### 启动后端

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

前端工作台地址：

```text
http://localhost:8080/
```

Postman 文件也已经放在仓库里：

- `docs/postman/SmartHire.postman_collection.json`
- `docs/postman/SmartHire.local.postman_environment.json`
- `docs/postman/README.md`

### 启动独立前端

如果你要使用独立前端开发：

```bash
cd frontend
npm install
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

当前独立前端已接通：

- 公共岗位浏览与角色化登录入口
- Candidate：简历上传、投递、我的申请、我的面试、我的通知
- HR：岗位创建/编辑/删除、查看申请、更新申请状态、安排/更新面试、统计概览
- Admin：全局统计、岗位总览、用户状态管理、操作日志筛选

### 运行后端测试

```bash
cd backend
mvn test
```

当前后端测试结果：

- `93` 个测试通过
- 覆盖认证、管理员用户管理、操作日志、简历上传、统计、岗位、投递、面试、通知、Redis 缓存、RabbitMQ 通知链、定时任务、Swagger 文档端点、前端欢迎页和基础安全规则
- 已补充基于 Testcontainers 的 Flyway / MySQL、Redis 缓存失效、RabbitMQ 异步通知集成测试

### GitHub Actions CI

仓库已经提供基础 CI 工作流：

- [ci.yml](/Users/jay/Projects/SmartHire/.github/workflows/ci.yml)

当前会自动执行：

- backend：`mvn test`
- frontend：`npm ci && npm run build`
- `docker compose config` 配置校验

仓库还补了镜像构建工作流：

- [docker-images.yml](/Users/jay/Projects/SmartHire/.github/workflows/docker-images.yml)

当前会自动验证：

- backend Docker 镜像可构建
- frontend Docker 镜像可构建

触发方式：

- push 到 `main`
- pull request
- 手动触发 `workflow_dispatch`

说明：

- `http://localhost:5173` 是独立前端容器，`/api` 会自动反向代理到后端容器
- `http://localhost:8080` 仍然保留 Spring Boot 同源提供的轻量 workbench

### GitHub Actions CD

仓库已经补上第一版 CD 骨架：

- [publish-images.yml](/Users/jay/Projects/SmartHire/.github/workflows/publish-images.yml)
- [deploy.yml](/Users/jay/Projects/SmartHire/.github/workflows/deploy.yml)
- [deploy/docker-compose.prod.yml](/Users/jay/Projects/SmartHire/deploy/docker-compose.prod.yml)
- [deploy/.env.example](/Users/jay/Projects/SmartHire/deploy/.env.example)
- [deploy/README.md](/Users/jay/Projects/SmartHire/deploy/README.md)

当前发布流程：

- `publish-images.yml` 会在 `CI` 成功后，或手动触发时，把 `backend` / `frontend` 镜像推送到 `GHCR`
- 默认会推送 `latest` 和基于 commit sha 的标签

当前部署流程：

- `deploy.yml` 通过 `workflow_dispatch` 手动触发
- workflow 会把生产 compose 文件复制到远程服务器
- 然后根据 `PROD_ENV_FILE` 和镜像标签生成远程 `.env`
- 最后在服务器执行 `docker compose pull && docker compose up -d`

部署需要的 GitHub Secrets：

- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY`
- `DEPLOY_PORT`
- `DEPLOY_PATH`
- `GHCR_USERNAME`
- `GHCR_TOKEN`
- `PROD_ENV_FILE`

说明：

- `PROD_ENV_FILE` 可以参考 [deploy/.env.example](/Users/jay/Projects/SmartHire/deploy/.env.example) 准备
- 当前这版属于“可接服务器的 CD 骨架”，还没有替你绑定具体服务器、域名或 HTTPS

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
- 查看自己岗位范围内的基础统计

### ADMIN

- 具备 HR 能力
- 可跨岗位管理业务数据
- 可查看全局统计概览
- 可分页查看全部用户并调整账号状态
- 可查看关键业务操作日志

## 如何创建 HR / ADMIN 账号

当前注册接口支持自助创建 `CANDIDATE / HR` 账号，`ADMIN` 账号仍然建议通过演示数据或手动绑定角色的方式准备。如果你已经执行了 `sql/002_seed_demo_data.sql`，可以直接使用下面的演示账号。

| 角色 | 邮箱 | 密码 |
| --- | --- | --- |
| Candidate | `candidate@example.com` | `password123` |
| Candidate | `candidate2@example.com` | `password123` |
| HR | `hr@example.com` | `password123` |
| Admin | `admin@example.com` | `password123` |

如果你没有导入演示数据：

- 候选人和 HR 可以直接通过注册接口创建账号
- Admin 可以先创建普通用户，再手动绑定 `ADMIN` 角色

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

## 数据库说明

核心结构迁移已经落地在 [V1__init_schema.sql](/Users/jay/Projects/SmartHire/backend/src/main/resources/db/migration/V1__init_schema.sql)、[V2__add_operation_logs.sql](/Users/jay/Projects/SmartHire/backend/src/main/resources/db/migration/V2__add_operation_logs.sql) 和 [V3__add_notification_event_key.sql](/Users/jay/Projects/SmartHire/backend/src/main/resources/db/migration/V3__add_notification_event_key.sql)：

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
- 通知表通过 `event_key` 支撑 RabbitMQ 消费幂等
- 管理员可直接控制用户状态为 `ACTIVE / DISABLED`
- 管理员可查看关键写操作的审计日志

## 下一步建议

如果继续往下做，最值得补的顺序是：

1. 先补更细的统计图表或管理员可视化视图
2. 再补真实生产环境接入、域名 / HTTPS、回滚与环境分支策略
3. 继续做日报类定时任务
4. 最后视情况补邮件 / 短信等通知扩展消费者

## 补充资料

仓库里已经预留了演示说明与补充文档：

- [docs/demo-guide.md](/Users/jay/Projects/SmartHire/docs/demo-guide.md)
- [docs/screenshots/README.md](/Users/jay/Projects/SmartHire/docs/screenshots/README.md)
