# 后端架构文档

## 分层架构

```
Controller → Service → DAO (接口) → DAO (实现) → Mapper → DB
```

### 1. Controller 层
- 路径：`com.bintech.metrix.controller`
- 职责：请求转发、参数校验、调用 Service
- 禁止包含业务逻辑

### 2. Service 层
- 路径：`com.bintech.metrix.service` (接口) / `service.impl` (实现)
- 职责：业务逻辑编排、事务管理
- 只注入 DAO 接口，不注入 Mapper

### 3. DAO 层（持久化入口）
- 路径：`com.bintech.metrix.repository.dao`
- 接口（public）：对外暴露的持久化方法
- 实现（package-private + `@Repository`）：参数校验、条件构造

#### 设计原则
- **禁止** DAO 接口接受 `LambdaQueryWrapper` / `LambdaUpdateWrapper` 参数
- 必须为每个查询定义具体方法，如 `selectByUserId(Long)`、`countByUsernameAndNotId(String, Long)`
- 实现类必须校验参数（非空、非空白），校验失败时 `log.warn` + 返回安全默认值（`null`、`List.of()`、`0`）
- 条件构造器（`LambdaQueryWrapper`/`LambdaUpdateWrapper`）仅在 DAO 实现类内部构建
- Mapper 仅在 DAO 实现类中注入，Service/Controller 不可见

### 4. Mapper 层
- 路径：`com.bintech.metrix.repository.mapper`
- 职责：MyBatis-Plus Mapper 接口，仅 DAO 实现类注入

## 审计字段自动填充

| 字段 | 填充时机 | 填充值 |
|---|---|---|
| `create_time` / `createTime` | INSERT | `LocalDateTime.now()` |
| `update_time` / `updateTime` | INSERT, UPDATE | `LocalDateTime.now()` |
| `creator` | INSERT | `StpUtil.getLoginId()` 或 `"UNKNOWN"` |
| `modifier` | INSERT, UPDATE | `StpUtil.getLoginId()` 或 `"UNKNOWN"` |

- 通过 `MybatisMetaObjectHandler`（`com.bintech.metrix.config`）自动填充
- 使用 `strictInsertFill` / `strictUpdateFill`，实体显式设置的值不会被覆盖
- `creator`/`modifier` 通过 `StpUtil.getLoginId()` 获取当前登录用户 ID，未登录时使用 `"UNKNOWN"` 并输出 warn 日志
- 实体基类 `BaseEntity` 的 `@TableField` 需标注 `fill = FieldFill.INSERT` / `fill = FieldFill.INSERT_UPDATE`

## Python 进程调用规范

所有通过 `ProcessBuilder` 调用 Python 脚本的代码必须遵循：

```java
ProcessBuilder pb = new ProcessBuilder(command);
pb.redirectErrorStream(true);
pb.environment().put("PYTHONIOENCODING", "utf-8");  // 强制 Python 输出 UTF-8，避免 Windows 中文乱码
Process process = pb.start();

// 读取输出流必须使用 UTF-8
BufferedReader br = new BufferedReader(
    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

// 必须设置超时
boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
if (!finished) {
    process.destroyForcibly();
}
```

### 为什么需要设置 `PYTHONIOENCODING`
- Windows 中文环境下 Python 的 stdout 默认使用 GBK 编码
- Java 端使用 `StandardCharsets.UTF_8` 读取输出流
- 编码不匹配导致中文乱码
- 设置 `PYTHONIOENCODING=utf-8` 强制 Python 以 UTF-8 输出，保持一致

### 涉及文件
- `MarketDataServiceImpl.java` — 实时行情、K线、筹码、十大流通股东
- `NewsServiceImpl.java` — AKShare 新闻采集
- `MarketIndexServiceImpl.java` — 大盘指数
- `MarketActivityServiceImpl.java` — 赚钱效应分析
- `MarketReviewDataFetcher.java` — 盘后总结指数数据

## 新闻数据流

```
AKShare Python 脚本 / Bocha API
    → NewsCollector.collect()
        → 解析 JSON 为 List<Map<String, Object>>
        → 按 publishTime 倒序排列（最新在前）
        → 调用 AI 生成摘要
        → 返回 newsSummaryMap
    → StockAnalysisServiceImpl.analyzeStock()
        → JSONUtil.toJsonStr() 序列化
        → 存入 DB (record.newsSummary)
    → StockAnalysisServiceImpl.getAnalysisDetail()
        → parseNewsList() 反序列化为 List<NewsItem>
        → 返回前端渲染
```

### 排序规则
- 新闻在 `NewsCollector.collect()` 中按 `publishTime` 字段倒序排列
- 缺失 `publishTime` 或值为"未知时间"的条目排到最后
- 前端直接展示，不额外排序
