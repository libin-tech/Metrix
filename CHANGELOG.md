# 变更记录

## 1.6.2 (2026-09-03)
### 官网与账号体系
- 新增中英文官网入口、免责声明与微信公众号联系方式
- 管理员改为账号、密码、图形验证码登录；普通用户改为邮箱注册和邮箱、密码、图形验证码登录
- 移除微信扫码登录链路，新增注册、忘记密码、邮箱验证码和密码强度校验
- 注册需同意隐私政策，记录 `privacy_agreed` 与 `privacy_agreed_time`，新增 `V1.6.3_user_privacy_agreement.sql` 升级脚本

### 安全与权限
- 图形验证码支持通过 `AUTH_CAPTCHA_ENABLED` 在本地联调时统一跳过，生产环境默认启用
- 邮箱验证码发送改为图形验证码弹窗确认，前后端均限制每个邮箱每分钟一次
- 邮件使用独立执行器异步投递，内容改为带品牌说明的 HTML 邮件
- 修复未配置角色关联的普通用户登录后权限为空的问题，并阻止普通用户访问管理端前端路由

### 配置与文档
- 完善 `backend/.env.example`，补充 Redis、SMTP、验证码和市场数据脚本配置说明
- 更新中英文 README 的认证接口、升级步骤与部署配置说明

### 功能移除
- 移除分析报告 PDF 导出入口、接口、服务实现、依赖与权限注册；新增 `V1.6.4_remove_pdf_export.sql` 清理已部署环境中的权限数据

## 1.6.1 (2026-06-17)
### 持久层重构
- 新增 19 个 DAO 接口（拒绝 `LambdaQueryWrapper`/`LambdaUpdateWrapper` 参数，全部改为具体方法）和包级私有实现类
- 重构所有 15 个调用方（Service/Controller/Config）使用具体 DAO 方法，消除 400+ 编译错误
- 新增 `MybatisMetaObjectHandler`，通过 `StpUtil.getLoginId()` 自动填充 `creator`/`modifier`
- 实体基类 `BaseEntity` 添加 `FieldFill` 注解
- 补充 DAO 接口缺少的方法：`countByPermissionCode`、`countByPermissionCodeExcludeId`、`deleteByApiId`、`selectAll`

### Python 编码修复
- 所有 5 个 `ProcessBuilder` 调用 Python 脚本处设置 `PYTHONIOENCODING=utf-8`
- 修复 Windows 中文环境下 Python 输出 GBK 导致的中文乱码

### Bug 修复
- 修复 `NotificationConfigDaoImpl` 引用了不存在的 `getNotifyType()` 字段
- 修复 `SystemApiServiceImpl` 引用了不存在的 `selectAllActive()` 方法

### 功能优化
- 评估概览新闻列表按 `publishTime` 倒序排列（最新在前）

### 代码清理
- 删除未使用的私有方法和常量
- 删除冗余 import
- 删除注释掉的代码
- 删除上一次重构残留的 DAO 包装器引用

### 文档完善
- 新增 `backend/.doc/ARCHITECTURE.md` 架构文档
- 更新 `backend/AGENTS.md` 分层、Python 进程、审计字段规范
- 更新 `AGENTS.md` 项目级规范引用

## 1.6.0 (2026-06-10)
### SaaS 多租户隔离
- 为 BrokerAccount、PortfolioHolding、MarketReview、AiModelConfig、MarketDataConfig、NewsSourceConfig、NotificationConfig、ChatMessage 新增 `user_id` 字段做数据隔离
- 所有 service 接口增加 userId 重载，异步任务显式传递 userId 避免虚拟线程丢失 ThreadLocal
- Chat 模块全面隔离：会话/消息操作校验所属权

### 组合持仓价格缓存
- PortfolioHolding 新增 `cached_price` / `cached_price_time` 字段，列表页即时展示无需等待刷新

### AOP 配置完整性检查
- 新增 `@CheckConfig` 注解和 `ConfigCheckAspect`，替代控制器内联 if 判断
- StockAnalysisController、ChatController、PortfolioController、MarketReviewController 统一使用注解

### 移除日限额 & UsageStats
- 删除 `usage_stats` 表及全部后端代码（entity、mapper、service、controller）
- 前端删除 Stats.vue、路由、菜单、API 调用、i18n 条目

### 移除审计日志模块
- 删除 `audit_log` 表相关 DDL/DML 及全部后端代码（entity、mapper、service、controller、annotation、aop）
- 前端删除 AuditLog.vue、路由、菜单、API 调用、i18n 条目
- 更新 `system_api.sql` 和 `system_menu.sql` 移除审计相关注册数据

### 代码清理
- 删除未使用的 `UsageStatsVO`、`AuditLogVO`、`MenuTypeEnum`
- 删除前端遗留的 `Stats.vue` 页面及关联引用
- 清理冗余模型类（`CheckList`、`PositionAdvice`）


## 1.5.0 (2026-05-29)
### 首页重构
- 重构 Home 页面布局：顶部三栏（大盘行情 + 赚钱效应 + 数据看板）置于同一 flex 行等高对齐
- 底部区域分为最近评估记录（左）与快速操作 + 最近复盘记录（右）
- 大盘行情指标数值/涨跌幅字号放大加粗，赚钱效应标签/柱状条加大尺寸更突出

### 数据轮询优化
- 新增 `isTradingTime()` 判断：交易日 09:30-11:30 / 13:00-15:00 内每 20 秒自动刷新大盘行情与赚钱效应
- 非交易时间（周末/休市）仅加载一次，避免无效请求

## 1.4.0 (2026-05-27)
### 权限管理系统
- 新增 RBAC 权限系统：系统角色、系统菜单、系统接口、角色-菜单-接口三级关联
- Sa-Token 升级至 1.45.0，注解鉴权：所有 Controller 方法统一添加 `@SaCheckPermission`
- 全局异常处理新增 `FrozenUserException`（code=1001）、`NotPermissionException`、`NotRoleException`
- 后端新增 `SaAnnotationInterceptor` 处理 `@SaCheckPermission` 注解
- 新增 `GET /api/auth/permissions` 接口返回用户权限码列表
- 新用户注册自动分配 USER 默认角色

### 管理后台
- 用户管理：列表展示角色名称，冻结用户弹窗填写原因，隐藏管理员用户
- 角色管理：菜单/接口权限分两个 Tab 独立保存，全选/取消全选功能
- 菜单管理：树形展示、增删改、关联接口、上级菜单选择（过滤按钮节点）
- 接口管理：完整 100+ 接口注册数据（`.doc/db/system_api.sql`）
- 数据统计：今日/日期范围使用统计（标的评估数+复盘数）
- 审计日志：分页查询，操作类型着色，时间范围筛选

### 前端优化
- 侧边栏菜单按权限动态展示，无权限项自动隐藏
- 新增默认首页：管理员展示数据看板，普通用户展示欢迎页+使用指南
- 右上角展示用户昵称（微信昵称优先，用户名 fallback）
- 侧边栏 Logo 改为纯 CSS "M" 字母图标
- 所有硬编码中文替换为 `$t()` 国际化，新增 `welcome`/`common` i18n 段

### 接口与数据
- axios 响应拦截器增加业务 `code` 校验和 403/1001 特殊处理
- 冻结用户 1001 错误码弹出 `Modal.error` 固定窗口提示
- 数据库脚本合并为 `ddl-init.sql`（完整 DDL）+ `dml-init.sql`（初始数据）
- 修复 `MenuCreateRequest`/`MenuUpdateRequest` 枚举校验 `@NotBlank`→`@NotNull`

### Bug 修复
- 修复菜单树/上级菜单 TreeSelect `replace-fields`/`field-names` 不生效问题
- 修复 `loadAnalysisRecords` 未定义导致的“开始评估”报错
- 修复登录后白屏：`fetchUser` 改为 `watch(route.path)` 触发
- 修复 Ant Design Vue Tree switcher/icon/title 重叠问题
- 修复 `onUnmounted` 误删导致的启动报错
- 修复 Sa-Token 注解鉴权失效（`SaInterceptor` 匿名子类覆写 `preHandle` 导致注解处理器被绕过）

## 1.3.0 (2026-05-24)
- 新增主题切换系统：提供天空蓝、翡翠绿、暮光紫、落日橙、极光青 5 种主题色，通过 Ant Design Vue ConfigProvider Design Token 实现全局生效
- 新增问一问功能增强：AI 分析步骤追踪（8步过程耗时与状态）、批量删除会话、Markdown 流式渲染
- 新增 SSE 数据解析健壮性修复：兼容 `data: ` 前导空格
- 后端重构：ChatService 从流式改为非流式 AI 调用 + 步骤记录持久化
- 数据库迁移文件合并：移除 V002 独立迁移脚本，所有变更统一归入 init.sql
- 前端修复：Chat.vue `:size="70"` 属性截断错误
- 前端优化：Chat.vue 重写会话选择与批量删除交互
- 新增大盘复盘模块：AI 驱动的 A 股主要指数日复盘，含自动定时任务
- 新增持仓管理模块：多账户管理、批量录入、一键评估、实时盈亏
- 新增系统 Logo：侧边栏和登录页统一品牌标识
- 国际化完善：中英文 key 完全同步，修复切换失效问题
- 概览页重构为三栏布局，优化价格概览条和密度
- AKShare 代理修复：自动清除子进程代理环境变量
- AKShare 指数接口迁移为 `stock_zh_index_daily` + 实时行情补充
- 修复 tqdm 进度条污染 Python 脚本 JSON 输出

## 1.1.0 (2026-05-18)
- 移除数据脱敏（Masking）功能，恢复完整数据展示
- 优化标的评估页面、首页及标的数据页面的显示逻辑

## 1.0.0 (2026-05-14)
- 初始化项目结构
- 实现 AI 股票分析核心功能
- 集成 TickFlow 实时行情
- 集成博查新闻数据
- 实现筹码分布计算（Python）
- 飞书通知集成
- Sa-Token JWT 认证
- 前端 Vue 3 + Ant Design Vue 界面
- 支持中英文国际化
- Docker 容器化部署
