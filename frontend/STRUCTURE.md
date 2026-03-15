# Frontend Structure

## 目标

这份结构面向 `SmartHire` 的独立前端，默认技术选型：

- `React`
- `TypeScript`
- `Vite`
- `React Router`
- `TanStack Query`
- `React Hook Form + Zod`

设计原则：

- 按业务模块拆分，而不是把所有逻辑堆到一个大页面里
- 页面负责组合，`features` 负责业务能力，`shared` 负责真正复用的基础能力
- 前端只处理界面、状态和交互，后端业务规则仍然以 Spring Boot 为准

## 推荐目录

```text
frontend/
├── public/
├── src/
│   ├── app/
│   │   ├── providers/
│   │   ├── router/
│   │   └── store/
│   ├── features/
│   │   ├── auth/
│   │   ├── jobs/
│   │   ├── resumes/
│   │   ├── applications/
│   │   ├── interviews/
│   │   ├── notifications/
│   │   ├── statistics/
│   │   └── admin/
│   ├── pages/
│   │   ├── public-jobs/
│   │   ├── candidate-dashboard/
│   │   ├── hr-dashboard/
│   │   └── admin-dashboard/
│   ├── shared/
│   │   ├── api/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── layouts/
│   │   ├── lib/
│   │   ├── styles/
│   │   └── types/
│   ├── main.tsx
│   └── vite-env.d.ts
├── .env.example
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 分层职责

### `src/app`

放应用级配置，不放具体业务页面。

- `providers/`
  - `QueryProvider`
  - `AuthProvider`
  - `ThemeProvider`，如果以后需要主题切换
- `router/`
  - 路由表
  - 路由守卫
  - 角色页面入口
- `store/`
  - 只放全局状态
  - 例如当前登录用户、token、全局筛选上下文

### `src/features`

这里是前端的业务核心，建议每个模块内部保持相似结构：

```text
features/jobs/
├── api/
├── components/
├── hooks/
├── schemas/
├── types/
└── utils/
```

建议各模块职责如下：

- `auth`
  - 登录
  - 当前用户信息
  - token 持久化
  - 基础角色判断
- `jobs`
  - 公共岗位列表
  - 岗位详情
  - HR 创建/编辑/删除岗位
  - Admin 岗位总览
- `resumes`
  - PDF 简历上传
  - 已上传路径展示
- `applications`
  - 候选人投递
  - 我的投递列表
  - HR 更新投递状态
- `interviews`
  - HR 安排面试
  - HR 更新面试
  - 候选人查看我的面试
- `notifications`
  - 通知列表
  - 未读数量
  - 标记已读
- `statistics`
  - HR 和 Admin 概览卡片
  - 图表或分布统计
- `admin`
  - 用户管理
  - 操作日志
  - 管理员岗位总览

### `src/pages`

页面目录只做“页面装配”，不要塞太多请求细节。

推荐路由：

- `/`
  - 公共岗位首页
- `/candidate`
  - 候选人工作台
- `/hr`
  - HR 工作台
- `/admin`
  - 管理员工作台

每个页面通常做三件事：

- 组合多个 `features`
- 组织布局和信息层级
- 处理页面级筛选状态

### `src/shared`

只放真正跨模块复用的内容。

- `api/`
  - 请求封装
  - token 注入
  - 统一错误处理
- `components/`
  - `Button`
  - `Card`
  - `EmptyState`
  - `StatusBadge`
  - `Table`
  - `Modal`
- `hooks/`
  - 通用 hooks
  - 比如 `useDebounce`、`usePaginationState`
- `layouts/`
  - 页面壳
  - 侧边栏、顶部栏、工作区布局
- `lib/`
  - 日期格式化
  - 金额格式化
  - 状态文案映射
- `styles/`
  - 全局变量
  - reset
  - typography
  - tokens
- `types/`
  - 通用类型
  - 分页响应
  - 公共 API 响应

## 推荐页面组合

### Public Jobs Page

- 岗位搜索栏
- 岗位列表
- 岗位详情卡
- 登录入口提示

### Candidate Dashboard

- 个人信息卡
- 简历上传
- 选中岗位投递
- 我的投递
- 我的面试
- 我的通知

### HR Dashboard

- 个人信息卡
- 岗位编辑器
- 岗位列表或上下文
- 投递列表
- 面试编辑器
- HR 统计概览

### Admin Dashboard

- Admin 信息卡
- 全局统计
- 岗位总览
- 用户管理
- 操作日志

## API 组织建议

推荐统一放到 `src/shared/api/client.ts`：

- 处理 `baseURL`
- 自动注入 `Authorization`
- 统一处理 `401`
- 统一把后端 `ApiResponse<T>` 解包

业务模块内只保留面向模块的方法，例如：

```text
features/jobs/api/listJobs.ts
features/jobs/api/getJobDetail.ts
features/jobs/api/createJob.ts
features/admin/api/listAdminJobs.ts
```

## 环境变量

建议只暴露前端真正需要的：

```env
VITE_API_BASE_URL=http://localhost:8080
```

开发时：

- 前端跑 `5173`
- 后端跑 `8080`

生产时：

- 前端部署静态资源
- 后端继续作为独立 Spring Boot API 服务

## 迁移建议

从当前仓库迁移到独立前端时，建议这样走：

1. 先保留 `backend/src/main/resources/static/` 里的轻量工作台
2. 新前端优先实现公共岗位页和候选人工作台
3. 再实现 HR 和 Admin 页面
4. 功能稳定后，再决定是否删除旧静态工作台

## 当前最适合的下一步

如果你接下来要正式开 `frontend/`，建议第一批直接做：

- `auth`
- `jobs`
- `applications`
- `interviews`
- `admin`

这样能最快把现在已有后端能力转成真正的前端产品形态。
