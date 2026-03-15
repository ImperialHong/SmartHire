# Backend Structure

## 目标

这份结构基于当前 `SmartHire` 后端现状整理，目标不是推翻现有代码，而是把现在已经形成的模块化风格明确下来，方便后续继续扩展。

当前技术核心：

- `Spring Boot`
- `Spring Security + JWT`
- `MyBatis-Plus`
- `MySQL`
- `Swagger / OpenAPI`

设计原则：

- `common` 放跨模块基础能力
- `config` 放框架配置
- `modules` 按业务域拆分
- 每个业务模块尽量保持清晰的 `controller -> service -> mapper/entity/dto` 流向

## 当前推荐目录

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/smarthire/
│   │   │   ├── common/
│   │   │   │   ├── api/
│   │   │   │   └── exception/
│   │   │   ├── config/
│   │   │   ├── modules/
│   │   │   │   ├── admin/
│   │   │   │   ├── application/
│   │   │   │   ├── auth/
│   │   │   │   ├── interview/
│   │   │   │   ├── job/
│   │   │   │   ├── notification/
│   │   │   │   ├── operationlog/
│   │   │   │   ├── resume/
│   │   │   │   ├── statistics/
│   │   │   │   └── system/
│   │   │   └── SmartHireApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── static/
│   └── test/
├── Dockerfile
├── pom.xml
└── STRUCTURE.md
```

## 核心分层

### `common`

只放跨模块都会用到的能力。

- `api/`
  - `ApiResponse`
  - `PageResponse`
- `exception/`
  - 业务异常
  - 全局异常处理

这里不要放业务规则。

### `config`

放框架级配置。

当前已经适合放在这里的内容包括：

- `SecurityConfig`
- `OpenApiConfig`
- `MybatisPlusConfig`
- 其他未来的跨模块配置
  - Redis
  - MQ
  - CORS
  - 文件存储配置

### `modules`

按业务域拆，而不是按技术类型横切全项目。

推荐继续保持现在这种风格：

```text
modules/job/
├── controller/
├── dto/
│   ├── request/
│   └── response/
├── entity/
├── mapper/
└── service/
    └── impl/
```

这类结构很适合当前项目规模，也比较容易讲清楚。

## 各业务模块职责

### `auth`

负责：

- 注册
- 登录
- JWT 签发与解析
- 当前用户身份解析
- 角色信息装配

建议长期只让认证相关逻辑留在这里，不把业务权限判断塞进别的模块。

### `job`

负责：

- 岗位创建、更新、删除
- 公共岗位列表与详情
- HR / Admin 岗位管理

如果以后加缓存，岗位缓存优先落这个模块。

### `application`

负责：

- 候选人投递
- 我的投递列表
- HR 查看岗位投递
- 投递状态流转

它是招聘主流程里的核心状态模块之一。

### `interview`

负责：

- 安排面试
- 更新面试
- 候选人查看面试
- 面试状态与结果

如果以后扩展成多轮面试，可以优先从这个模块演进。

### `notification`

负责：

- 通知生成
- 通知列表
- 未读数
- 标记已读

如果后面做 RabbitMQ，这里最适合先升级成异步通知链路。

### `statistics`

负责：

- HR 视角概览
- Admin 全局概览
- 业务状态分布统计

它更偏查询聚合层，不宜塞写操作。

### `resume`

负责：

- 简历上传
- 文件路径返回

如果以后接对象存储，也优先在这个模块扩展。

### `admin`

负责：

- 管理员用户管理
- 管理员岗位总览
- 未来轻量后台能力

当前项目里，`admin` 应保持“轻量管理层”，不要演化成复杂权限平台。

### `operationlog`

负责：

- 关键写操作日志记录
- 管理员日志查看

这是很适合继续增强的模块，比如以后可加操作来源、请求追踪 ID、旧值/新值摘要。

### `system`

负责：

- 健康检查
- 系统级辅助接口

## DTO 约定

建议继续保持：

- `request` 和 `response` 分开
- 控制器不要直接暴露实体类
- service 返回 `response`

这样做的好处：

- 接口边界清楚
- 后面前端联调更稳定
- 数据库字段调整不会直接外溢到 API

## Service 约定

建议统一：

- `controller`
  - 参数校验
  - 获取当前登录用户
  - 调用 service
- `service`
  - 业务规则
  - 权限校验
  - 状态流转
  - 日志记录
- `mapper`
  - 数据读写

当前项目里，“RBAC + 数据归属校验” 这套逻辑应该继续留在 service 层。

## 测试结构建议

当前后端已经有比较完整的测试基础，后续建议继续保持两层：

- `controller security tests`
  - 验证未登录、错误角色、正确角色
- `service tests`
  - 验证状态流转和业务规则

以后如果继续增强，可以补：

- mapper 集成测试
- 文件上传集成测试
- Docker 环境下的端到端 smoke test

## 后续扩展建议

### 如果继续做 P2

建议只先选一个工程亮点：

- `Redis`
  - 加到 `job`、`statistics`
- `RabbitMQ`
  - 加到 `notification`

### 如果前后端独立部署

后端结构本身不需要大改。

变化主要是：

- `resources/static/` 的轻量工作台可以保留，也可以未来下线
- 后端彻底转成纯 API 服务
- 新前端通过环境变量访问 `/api`

## 当前最适合保持的风格

你现在这套后端最好的地方，是已经形成了“按业务模块拆分”的基础秩序。  
所以后面不要把它改回“所有 controller 一堆、所有 service 一堆”的扁平结构。

一句话总结：

`继续按模块演进，在现有结构上增强，而不是为了“更像模板项目”去重构掉已经清楚的业务边界。`
