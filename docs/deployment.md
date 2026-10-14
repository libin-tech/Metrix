# Metrix 部署指南

> [English](./deployment_en.md) | 中文 · [返回功能介绍](../README.md)

本文包含环境准备、本地开发、数据库初始化与升级、Docker 部署、前端单独部署，以及配置和开发参考。公网服务器、HTTPS、备份与故障排查详见[公网部署指南](./public-deployment.md)。

以下命令从仓库根目录执行，除非命令中已明确切换目录。本地开发使用 `backend/.env`，Docker 部署使用根目录 `.env`。

## 技术栈

| 层级 | 技术                                              |
|------|-------------------------------------------------|
| 后端 | Java 21, Spring Boot 4.0.6, MyBatis-Plus 3.5.16 |
| 数据库 | PostgreSQL 16                                   |
| AI | LangChain4j 1.15.0 (OpenAI / Ollama)            |
| 认证 | Sa-Token 1.45.0 (JWT, 注解鉴权)                  |
| 前端 | Vue 3, Ant Design Vue, Vite                     |
| 数据源 | 同花顺金融数据 API (行情), 博查 (新闻), AKshare (基础数据)          |
| 工具 | Hutool 5.8.44, Lombok                           |

## 快速开始

### 环境要求

- JDK 21
- Maven 3.9+
- Node.js 22+
- PostgreSQL 16
- Redis 7+
- Python 3.10+（用于行情和分析脚本）

### 本地开发

```bash
# 0. 安装行情脚本依赖，并确保 PostgreSQL 和 Redis 已启动
pip install -r python-service/requirements.txt

# 1. 配置数据库
cp backend/.env.example backend/.env
# 编辑 backend/.env 填入 PostgreSQL、Redis 和 SMTP 配置

# 2. 创建并初始化全新数据库（已有数据库请参阅升级说明）
createdb -h 127.0.0.1 -U postgres stock_analysis
psql -h 127.0.0.1 -U postgres -d stock_analysis -f backend/.doc/db/ddl-init.sql
psql -h 127.0.0.1 -U postgres -d stock_analysis -f backend/.doc/db/dml-init.sql
psql -h 127.0.0.1 -U postgres -d stock_analysis -f backend/.doc/db/system_menu.sql
psql -h 127.0.0.1 -U postgres -d stock_analysis -f backend/.doc/db/system_api.sql

# 3. 启动后端
mvn -f backend/pom.xml clean package -DskipTests
java -jar backend/target/metrix-1.6.1.jar

# 4. 启动前端（新终端）
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，后端 API 在 `http://localhost:8080`。

### 已有数据库升级

先备份现有数据库，按待发布版本提供的实际迁移脚本升级。完整初始化 SQL 位于 `backend/.doc/db/`，仅用于新数据库，不应在已有数据库重复导入。详见 [公网部署指南中的升级与回滚](./public-deployment.md#升级与回滚)。

### Docker 部署

复制根目录环境变量模板，设置独立的数据库、Redis 和管理员密码：

```bash
cp .env.example .env
chmod 600 .env
# 编辑 .env，填写 POSTGRES_PASSWORD、REDIS_PASSWORD、ADMIN_PASSWORD。
docker compose config --quiet
docker compose up -d --build --wait --wait-timeout 300
```

访问 `http://127.0.0.1:8088`。首次空数据卷会自动初始化表、账号、菜单和接口权限；已有数据卷不会重复初始化。服务包括前端、Java/Python 后端、PostgreSQL 和 Redis，只有前端回环地址映射到宿主机。

公网部署在 `.env` 中填写 `METRIX_DOMAIN` 和 `ACME_EMAIL`，配置 DNS 与 80/443 安全组后执行：

```bash
docker compose -f docker-compose.yml -f docker-compose.public.yml up -d --build --wait --wait-timeout 300
```

完整的服务器准备、SMTP、HTTPS、验证、备份、升级和故障排查步骤见 [公网部署指南](./public-deployment.md)。

### 前端单独部署（非 Docker）

```bash
cd frontend

# 安装依赖
npm install

# 构建生产包
npm run build

# 方式一：使用 nginx 部署（推荐）
# 将 dist/ 目录拷贝到 nginx 的 html 目录下，并配置 nginx.conf 代理 /api 到后端

# 方式二：使用 Vite 预览（仅开发/测试）
npm run preview
```

前端默认运行在 `http://localhost:4173`（preview）或 nginx 配置的端口，后端 API 需可访问。

## 项目结构

```
Metrix/
├── backend/                 # Java Spring Boot 后端
│   ├── src/main/java/com/bintech/metrix/
│   ├── src/main/resources/  # 应用配置
│   ├── .doc/db/             # 表结构、账号、菜单和接口种子数据
│   └── Dockerfile
├── frontend/                # Vue 3 前端与 Nginx 配置
│   └── src/
│       ├── views/
│       ├── components/
│       ├── api/
│       ├── router/
│       ├── composables/
│       └── i18n/
├── python-service/          # 后端调用的 Python 行情脚本
├── deploy/                  # HTTPS 和管理员初始化配置
├── docs/                    # 部署文档
│   ├── deployment.md
│   ├── deployment_en.md
│   └── public-deployment.md
├── docker-compose.yml
├── docker-compose.public.yml
└── .env.example             # Docker 环境变量模板
```

## 配置说明

系统通过环境变量配置，支持 `.env` 文件：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `POSTGRES_URL` | `jdbc:postgresql://127.0.0.1:5432/stock_analysis` | 数据库连接地址 |
| `POSTGRES_USERNAME` | `postgres` | 数据库用户名 |
| `POSTGRES_PASSWORD` | `postgres` | 数据库密码 |

### 认证、验证码与邮件

完整配置示例见 [`backend/.env.example`](../backend/.env.example)。普通用户注册时必须勾选隐私政策；系统将把同意状态和时间写入 `users.privacy_agreed`、`users.privacy_agreed_time`。

- `AUTH_CAPTCHA_ENABLED=true`：管理员登录、普通用户登录和发送邮箱验证码均须校验图形验证码。仅本地联调可设为 `false`，前后端会同时跳过该校验。
- `MAIL_HOST`、`MAIL_PORT`、`MAIL_USERNAME`、`MAIL_PASSWORD`、`AUTH_MAIL_FROM`：SMTP 发信配置。邮箱验证码发送采用独立执行器，并在服务端限制同一邮箱每分钟一次。
- 普通用户登录后自动获得非管理端权限；管理端路由与 `/api/admin/**` 接口仅向管理员开放。

### AI 模型配置

通过前端配置页可管理 AI 模型：

- **OpenAI 兼容**：填写 API Base URL、API Key、模型名称
- **Ollama**：填写 Ollama 服务地址和模型名称

### 数据源配置

- **同花顺金融数据 API（FUYAO）**：基础报价、前复权日 K 和当日指数成交额。市场数据配置中填写 `https://fuyao.aicubes.cn` 和新的 API Key；旧行情 Key 不通用。换手率、量比暂缺，显示为空。历史成交额仍使用 Baostock。文档：https://fuyao.aicubes.cn/docs/
- **博查**：新闻数据，需配置 API Key
- **标的数据**：配置并启用同花顺金融数据 API 后，在标的数据页面点击“同步”。按完整股票代码新增或更新 A 股名称和代码，未变化记录不写库；已有行业、上市日期等信息保留。未配置时提供前往配置页的引导。

> **注意**：Windows 下使用 AKShare 时如遇网络代理错误，系统会自动清除子进程的 `HTTP_PROXY`/`HTTPS_PROXY` 环境变量，确保直连外网。

## API 概览

所有 API 以 `/api` 为前缀，部分接口需要登录认证（Sa-Token JWT）。共 100+ 个 API 端点，完整列表见 `backend/.doc/db/system_api.sql`。

### 业务接口

| 端点 | 说明 |
|------|------|
| `POST /api/auth/admin/login` | 管理员账号、密码、图形验证码登录 |
| `POST /api/auth/user/login` | 普通用户邮箱、密码、图形验证码登录 |
| `POST /api/auth/user/register` | 普通用户注册（需邮箱验证码和隐私政策同意） |
| `POST /api/auth/user/password/reset` | 校验邮箱验证码后重置普通用户密码 |
| `POST /api/auth/email-code` | 发送注册或重置密码邮箱验证码（受图形验证码及频率限制） |
| `GET /api/auth/captcha` | 获取图形验证码 |
| `GET /api/auth/verification-config` | 获取图形验证码启用状态 |
| `GET  /api/auth/permissions` | 获取当前用户权限列表 |
| `POST /api/analysis` | 提交分析任务 |
| `GET  /api/analysis` | 分析记录列表 |
| `GET  /api/analysis/{id}/detail` | 分析报告详情 |
| `POST /api/market-review/trigger` | 发起大盘复盘 |
| `GET  /api/market-review` | 复盘记录列表 |
| `POST /api/chat/session` | 创建对话会话 |
| `GET  /api/chat/sessions` | 会话列表 |
| `POST /api/chat/send` | 发送消息（SSE 流式响应） |
| `GET  /api/portfolio/holdings` | 持仓列表 |
| `POST /api/portfolio/holdings/batch` | 批量新增持仓 |
| `GET  /api/stock-basic/page` | 标的数据（分页） |
| `POST /api/stock-basic/sync` | 增量同步 A 股标的（需金融数据 API 配置） |
| `GET  /api/stocks/search` | 标的搜索 |

### 管理接口

| 端点 | 说明 | 权限 |
|------|------|------|
| `GET  /api/admin/users` | 用户列表 | `system:user:list` |
| `PUT  /api/admin/users/{id}/freeze` | 冻结用户 | `system:user:freeze` |
| `GET  /api/admin/roles` | 角色列表 | `system:role:list` |
| `POST /api/admin/roles/{id}/menus` | 分配菜单权限 | `system:role:assign-menu` |
| `POST /api/admin/roles/{id}/apis` | 分配接口权限 | `system:role:assign-api` |
| `GET  /api/admin/menus/tree` | 菜单树 | `system:menu:list` |
| `POST /api/admin/menus` | 创建菜单 | `system:menu:create` |
| `GET  /api/admin/apis` | 接口列表 | `system:api:list` |

## 开发指南

- 遵循 MyBatis-Plus 条件构造器方式进行数据库操作，禁止手写 SQL
- 实体类继承 `BaseEntity`，使用 Lombok 注解
- 控制器只做参数校验和请求转发，业务逻辑在 Service 层；Service 通过 DAO 访问数据，Mapper 仅由 DAO 实现类使用
- 使用构造函数注入，禁止 `@Autowired`
- 数据库变更脚本统一放在 `backend/.doc/db/` 下
- 完整开发规范见[全局规范](../AGENTS.md)、[后端规范](../backend/AGENTS.md)和[前端规范](../frontend/AGENTS.md)
