# “赚钱效应分析”替代数据源调研

调研日期：2026-08-28

## 结论

推荐把生产主数据源切换为 **Tushare Pro `daily_basic`**，用于收盘后的稳定展示。它在单个全市场结果中已直接给出 `limit_status`，不必再依赖乐咕乐股页面或由价格猜测涨跌停。统计口径如下：

| 页面字段 | `limit_status` 聚合 |
| --- | --- |
| 上涨数 | `1 + 2 + 3` |
| 下跌数 | `4 + 5 + 6` |
| 涨停数 | `2 + 3` |
| 跌停数 | `5 + 6` |

其中 1/4 是非涨跌停的上涨/下跌，2/5 是非一字涨跌停，3/6 是一字涨跌停；`0` 为平盘。以上状态定义由 Tushare 的官方 `daily_basic` 文档明确给出。[官方文档](https://tushare.pro/document/2?doc_id=32)

该接口交易日约 15:00–17:00 更新，因此它**不能满足盘中实时**“上涨/下跌/涨停/跌停”统计。若页面必须盘中更新，应采购 Tushare `rt_k` 实时日线权限，并与 `stk_limit` 的每日涨跌停价联表计算；不要把东方财富或 AKShare 的抓取接口当作高可用生产主源。

## 需求与口径

目标仅为 A 股四个数量字段。实现前须固定并在接口响应中带出：交易日、数据时间、市场范围（建议沪深京已上市普通 A 股；不含 B 股、基金、指数）、以及“涨停”是否包含 ST/一字板。原乐咕乐股结果同时含 `真实涨停/真实跌停` 和 ST 拆分；若改用以下聚合，默认统计的是范围内的**全部**涨跌停。

## 候选方案比较

| 候选 | 上涨/下跌 | 涨停/跌停 | 时效与授权 | 判断 |
| --- | --- | --- | --- | --- |
| **Tushare `daily_basic`** | 直接由收盘状态聚合 | 直接由收盘状态聚合 | 交易日 15–17 点更新；至少 2000 积分 | **收盘后首选**；一请求即可获得四项且口径清晰 |
| **Tushare `rt_k` + `stk_limit`** | `close` 与 `pre_close` 比较 | `close == up_limit/down_limit`（按价格精度比较） | `rt_k` 为单独的实时日线权限，官方价格表列为 200 元/月、50 次/分钟、单次全市场；`stk_limit` 需 2000 积分 | **盘中首选**；有正式权限、可控限流；需要维护联表和证券范围 |
| AKShare `stock_zh_a_spot_em` + 东财涨/跌停股池 | 现价/涨跌幅聚合 | 可数东财涨停/跌停池条数 | 免费，但均为抓取东方财富页面背后的未公开接口 | 仅可作为降级或验证源；不解决“上游公开页面易变/反爬”根因；股池本身排除 ST、科创板 |
| 直接调用东方财富 `push2` | 可从行情列表的涨跌幅聚合 | 需额外抓涨停、跌停池或未文档化字段 | 东方财富没有面向该用途的公开 API 契约 | 不建议作为生产承诺；与 AKShare 东财路径是同一类上游风险 |
| BaoStock | 可在盘后逐股票日线计算 | 没有官方的全市场涨跌停状态/涨跌停池接口 | 官方定位为历史 K 线；须遍历股票 | 不适合作为本功能主源；可用于收盘后抽样核对 |
| 更高等级 Tushare 打板接口 | 不适合替代全市场涨跌数 | 可获取涨跌停榜单/池 | `limit_list_ths` 需 8000 积分；使用受数据版权约束 | 仅在需要榜单详情时追加，不能替代四项汇总主数据 |

### 1. Tushare `daily_basic`（推荐的收盘后方案）

官方文档称该接口返回“全部股票”每日指标，单次最多 6000 条，交易日 15:00–17:00 更新；`limit_status` 精确定义了平盘、上涨、下跌、涨停、跌停和一字板六类收盘状态，且接口最低为 2000 积分。[接口说明与字段定义](https://tushare.pro/document/2?doc_id=32)；[积分与频次说明](https://tushare.pro/document/2?doc_id=290%EF%BC%89%EF%BC%9A)

接入应只请求 `ts_code,trade_date,limit_status`，在服务端按上表一次性分组计数。另以本地证券主数据筛出产品定义的 A 股范围，避免把不在页面口径内的品种混入。写入最近一次成功结果与 `asOf`；非交易日返回最近交易日数据，并明确标示日期，不能伪装成实时。

优点是无需保存任何第三方页面 Cookie，也没有乐咕乐股 HTML 表格的解析风险。限制是盘中无数据，且积分不是 API 密钥本身；实际凭据必须经环境变量/密钥管理注入，不能写入仓库或日志。

### 2. Tushare `rt_k` + `stk_limit`（推荐的盘中方案）

Tushare 官方资料列出 `rt_k` 为“实时日线”，支持代码通配符一次获得全市场；权限表写明其从 09:30 起提供当日实时日线、50 次/分钟、单次全市场，并为单独开通的 200 元/月权限。[接口目录（`rt_k`）](https://github.com/waditu/tushare-data/blob/master/tushare/references/%E6%95%B0%E6%8D%AE%E6%8E%A5%E5%8F%A3.md)；[官方权限表](https://tushare.pro/document/2?doc_id=290%EF%BC%89%EF%BC%9A)

`stk_limit` 在每个交易日约 09:00 更新全市场涨、跌停价格，单次最多 5800 条，并提供 `up_limit`、`down_limit`、`pre_close`。[官方 `stk_limit` 文档](https://tushare.pro/document/2?doc_id=183)

建议流程：

1. 每个交易日开盘前缓存当日 `stk_limit`，并在首个调用时校验证券数。
2. 盘中以 30–60 秒节流请求全市场 `rt_k`，与缓存按 `ts_code` 内连接。
3. `close > pre_close` 计上涨，`close < pre_close` 计下跌；以数据源返回的同一价格精度比较 `close` 和 `up_limit/down_limit` 计涨停/跌停，并排除无成交、停牌和产品口径外证券。
4. 使用上次成功快照降级，并返回 `source`、`asOf`、`stale`；请求失败不能再静默返回全零或空对象。

这个方案是付费的，但 API、时效、限频都有一方文档，适合承担页面的可靠性要求。

### 3. AKShare 与东方财富公开网页路径（可用但不推荐作主源）

当前项目失效的是 AKShare `stock_market_activity_legu()`，其上游为乐咕乐股网页。AKShare 自身更新日志曾多次记录修复该接口，说明它是适配网页而非稳定数据契约。[AKShare 变更记录](https://akshare.akfamily.xyz/changelog.html)

AKShare 的官方开源代码显示 `stock_zh_a_spot_em()` 调用 `push2.eastmoney.com/api/qt/clist/get`，目标页面是东方财富的沪深京 A 股行情页，并返回实时“涨跌幅”等字段。[AKShare 官方源码](https://github.com/akfamily/akshare/blob/main/akshare/stock_feature/stock_hist_em.py) 因此它可以用全量行情的涨跌幅计算上涨/下跌；其官方教程也列出东财涨停池和跌停池函数（`stock_zt_pool_em`、`stock_zt_pool_dtgc_em`）。[AKShare 官方教程](https://akshare.akfamily.xyz/tutorial.html)

但东方财富没有针对这些 `push2` 查询参数和字段的公开、版本化 API 文档。AKShare 官方 issue 中也记录过该行情请求的连接中断/失败；这不是东方财富的服务承诺，不能作为可靠性证据。[AKShare 官方 issue #7346](https://github.com/akfamily/akshare/issues/7346)

更具体地说，AKShare 的官方涨跌停池源码是 `stock_zt_pool_em(date)` 与 `stock_zt_pool_dtgc_em(date)`；源码注释说明其股池不包含 ST 和科创板，跌停池还只支持最近 30 个自然日。因此即使网络可用，直接数股池也与“全 A 股、包含 ST”的默认口径不同。[AKShare 官方涨跌停池源码](https://raw.githubusercontent.com/akfamily/akshare/main/akshare/stock_feature/stock_ztb_em.py)

因此的明确定位是：可作为**无凭据的临时备用**，采用低频请求、短超时、熔断与最近快照；不能把“直接调用东财”视为与 Tushare 完全独立的备源，因为 AKShare 的东财实现已经是同一上游。

### 4. BaoStock（不适合作为本功能替代）

BaoStock 官网将自己定位为知识库/API 平台；其官方 PyPI 发布内容展示的核心行情能力为 `query_history_k_data_plus`，查询的是历史 A 股 K 线，返回 `close`、`preclose` 等字段。[BaoStock 官网](https://baostock.com/)；[官方 PyPI/API 示例](https://pypi.org/project/baostock/)

它没有一方文档证明可一次得到盘中全 A 现价、每日涨跌停价格或最终涨跌停状态。因此计算四个数必须先获取证券列表，再逐只拉取日线；这既无法提供盘中值，也不能可靠判定不同板块/ST 的涨跌停。它可用作盘后抽样比对，不应接入首页实时/收盘总览。

### 5. 其他付费榜单数据

若产品将来需要展示个股名单、连板、高度或炸板，而不只是四个数字，可评估 Tushare 的附加数据：

- `limit_list_ths` 提供同花顺每日涨跌停榜单，含“涨停池”和“跌停池”，但文档注明需 8000 积分以上，且仅限个人学习研究；商业使用要另行联系数据版权方。[官方文档](https://tushare.pro/document/2?doc_id=355)
- `kpl_list` 是开盘啦涨跌停数据，官方文档为 5000 积分接口。[官方文档](https://tushare.pro/document/2?doc_id=347)

它们都只补充“涨停/跌停”详情，不能替代 `daily_basic` 的全市场上涨/下跌状态；在商业化前还必须完成授权核验。

## 建议的落地顺序

1. **先恢复页面：** 接入 Tushare `daily_basic` 的上一交易日/收盘后快照；字段与当前前端的 `up`、`down`、`limitUp`、`limitDown` 直接对齐。
2. **若确有盘中需求：** 开通 `rt_k` 后在同一 provider 下增加实时刷新；盘前拉取 `stk_limit`，盘中联表计算。先以至少 3 个交易日和现有行情终端做数值对账，再切流。
3. **保留有限降级：** AKShare/东财只在 Tushare 调用不可用时低频尝试；缓存最后成功结果；响应增加数据来源、统计日期和过期状态。
4. **监控与验收：** 记录原始证券数、入统计证券数、四类数及其与平盘/停牌的关系；监控抓取失败、结果为空、证券数突变和快照过期。数据源异常应在页面显示“截至时间/暂用缓存”，不得静默空白。

## 证据范围

本报告仅将供应方的官方文档、供应方维护的官方开源代码/issue 作为事实依据。东方财富部分不存在可核验的一方 API 文档，故只陈述其官方网页和 AKShare 官方开源代码实际使用的公开网页接口，不将其称为正式 API。
