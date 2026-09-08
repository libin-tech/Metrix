# 扶摇 API 替换 TickFlow / AKShare 调研

调研日期：2026-09-07。范围：Metrix 当前源码实际调用，以及扶摇在线文档和官方 Financial-API 仓库的公开接口契约。未使用 API Key 调用业务接口；以下是文档级可行性判断，不代表延迟、可用率或数据一致性已验收。

## 结论

可以作为 Metrix 的候选行情源，并替换部分 AKShare 功能；目前不满足保留全部字段、全部功能的整体替换。

- **TickFlow：日 K 和基础报价适合优先试接。** 当前项目主流程只使用日 K，没有实际调用盘口深度或 WebSocket。报价还存在换手率、量比和数据时间的缺口；两市盘中成交额需要实测口径和更新时效。
- **AKShare：仅部分替换。** 指数、交易日历、龙虎榜、热榜、涨停/跌停/炸板池有对应能力，但数据来源及部分字段不同。十大流通股东、A 股个股新闻、行业资金流、强势股池、乐咕赚钱效应完整统计未找到等价公开接口。
- 建议先增加扶摇适配层并保留原数据源，完成交易时段对照后按功能切换；不建议直接删除 TickFlow / AKShare 依赖。

依据：[扶摇接口总览](https://fuyao.aicubes.cn/docs/api-reference/overview/)、[官方完整能力目录](https://github.com/HiThink-Tech/Financial-API/blob/main/docs/api/capability-map.md)、[行情契约](https://github.com/HiThink-Tech/Financial-API/blob/main/docs/api/endpoints-prices.md)、[特色数据契约](https://github.com/HiThink-Tech/Financial-API/blob/main/docs/api/endpoints-special-data.md)。缺失项表示在当前公开目录和字段中未发现，不能据此断言服务商内部没有。

## 与项目实际调用的映射

| 项目用途与代码依据 | 扶摇能力 | 替换判断 |
| --- | --- | --- |
| `python-service/tickflow_quotes.py`：最新价、开高低、昨收、量额、涨跌幅、换手率、振幅、量比、时间 | `/api/a-share/prices/snapshot` | 基础字段可映射；通用股票快照未提供换手率、量比、振幅，振幅可按明确公式另算 |
| `python-service/tickflow_klines.py`：日 K、成交额、本地 MACD | `/api/a-share/prices/historical` | 可适配；单标的、时间区间查询，返回后排序并截取所需条数，保留本地 MACD |
| `python-service/tickflow_chip.py`：根据报价估算筹码 | 同一股票快照 | 可沿用项目估算逻辑；这不是上游真实历史筹码分布，数据源切换不会改变该性质 |
| `MarketIndexServiceImpl`：TickFlow 沪深两市当日成交额 | `/api/a-share-index/prices/snapshot` 的 `turnover`；指数日 K 可作历史候选 | 文档字段支持；必须验证 `000001.SH` 与 `399001.SZ` 成交额范围和当前项目一致，并确认盘中时效 |
| `akshare_index.py`、`akshare_index_spot.py`：指数历史和盘中快照 | `/api/a-share-index/prices/historical`、`snapshot` | 可适配；核实上证、深证、创业板、科创 50 的实际返回 |
| `akshare_gdfx.py`：`stock_gdfx_free_top_10_em` | 未发现 A 股十大流通股东接口 | 保留 AKShare 或另找来源；基金持仓/持有人不能代替上市公司流通股东 |
| `akshare_news.py`：`stock_news_em` | 未发现 A 股个股新闻接口 | 保留 AKShare / 已有博查兜底；基金资讯、异动原因不等价于个股新闻 |
| `akshare_market_activity.py`：`stock_market_activity_legu` | 全市场报价、涨跌停池提供部分原料 | 可另建统计，不能直接复现活跃度、真实涨跌停、ST 和停牌等完整口径 |
| `akshare_market_insights.py`：`stock_lhb_detail_em` | `/api/a-share/special-data/dragon-tiger-list` | 有龙虎榜，但日期方式、原因字段等需适配，不能直接换 URL |
| 同文件：`stock_hot_rank_em` | `/api/a-share/special-data/hot-stock-list` | 功能可替换为同花顺热榜；不再是东方财富排名，价格和涨跌幅需补查行情 |
| 同文件：`stock_fund_flow_industry` | 有行业指数行情和成分股，未发现行业净流入接口 | 不可等价替换；板块涨跌幅不是行业资金流 |
| 同文件：涨停、跌停、炸板池 | `limit-up-pool`、`limit-down-pool`、`limit-break-pool` | 有对应股票池，但行业、连续跌停等现有展示字段不完整 |
| 同文件：`stock_zt_pool_strong_em` | 未发现强势股池接口 | 保留原来源；涨停池或飙升榜不等于强势股池 |
| 同文件：`tool_trade_date_hist_sina` | `/api/a-share/calendar/trading-days` | 近一年窗口足够当前“最近交易日”用途，不代表覆盖长期历史日历 |
| 同文件：主力资金流榜 | 未发现等价接口 | 当前代码实际上直接请求东方财富，删除 AKShare 也不会移除这项外部依赖 |

扶摇字段依据：[行情](https://fuyao.aicubes.cn/docs/api-reference/prices/)、[指数](https://fuyao.aicubes.cn/docs/api-reference/a-share-index/)、[特色数据](https://github.com/HiThink-Tech/Financial-API/blob/main/docs/api/endpoints-special-data.md)、[交易日历](https://github.com/HiThink-Tech/Financial-API/blob/main/docs/api/endpoints-calendar.md)。本地依据以表内文件及 `backend/src/main/java/com/bintech/metrix/service/impl/MarketDataServiceImpl.java`、`MarketIndexServiceImpl.java` 为准；README 中的数据源简介不足以代表当前代码。

## 关键接入差异

### 报价与 K 线

核心映射为 `open_price → open`、`high_price → high`、`low_price → low`、`prev_price → prev_close`、`turnover → amount`、`price_change → ext.change_amount`、`price_change_ratio_pct → ext.change_pct`；K 线用 `close_price → close`、`date_ms → timestamp`。扶摇成交量单位为股，成交额为原始货币，A 股按元处理。原来的列数组结构需要从 `data.item[]` 转换。

`price_change_ratio_pct=1.74` 表示 1.74%，不能再次乘 100。不要沿用“绝对值小于 1 就乘 100”的启发式：0.5% 同样是合法百分数。股票快照缺失的换手率/量比应保持缺失，不能填 0 误导展示或分析；换手率还需匹配口径的流通股本，量比也不能仅由当天累计成交量可靠补齐。振幅可按 `(high-low)/prev_close*100` 自行定义，需与旧源口径核对。[行情契约](https://github.com/HiThink-Tech/Financial-API/blob/main/docs/api/endpoints-prices.md)

扶摇个股与指数历史接口当前具体参数均只承诺 `1d`。个股默认前复权 `forward`，支持 `none/forward/backward`，单次窗口最多 10 年；指数不接受复权参数。项目传入的是 `count`，扶摇要求 `start/end`，不能把最近 60 根理解为最近 60 个自然日。需要查询足够窗口、处理分页/停牌、排序后截取，显式固定复权口径。[个股历史契约](https://github.com/HiThink-Tech/Financial-API/blob/main/docs/api/endpoints-prices.md)、[指数契约](https://github.com/HiThink-Tech/Financial-API/blob/main/docs/api/endpoints-index.md)

### 股票池、榜单不是字段完全相同的替代品

龙虎榜当前项目取最近 30 个自然日、按上榜日期和净买额排序；扶摇按交易日查询，要保留原行为需遍历相应交易日并合并缓存。`limit_reason` 是涨跌停原因，不等于原来的“上榜原因”。同花顺人气排名与东方财富排名也不能混接成同一来源的历史序列。跌停池未提供原来的“连续跌停”，通用三池字段也未提供原来的“所属行业”；要么补数，要么调整展示。[特色数据契约](https://github.com/HiThink-Tech/Financial-API/blob/main/docs/api/endpoints-special-data.md)

在线完整文档另明确龙虎榜 `change/net_rate` 使用小数比例，与行情快照百分数不同；接入时应按端点转换，使用真实响应核对。[在线完整文档](https://fuyao.aicubes.cn/llms-full.txt)

### 文档差异、时效与商业条件

- 项目介绍仍提及日/周/月 K，但价格与指数的详细契约只支持日 K。应按后者设计，不能承诺分钟或周月接口已可用。[项目介绍](https://fuyao.aicubes.cn/docs/introduction/)、[行情契约](https://github.com/HiThink-Tech/Financial-API/blob/main/docs/api/endpoints-prices.md)
- 快照时间也存在描述差异：官方 GitHub 契约称显式 `thscodes` 查询时 `data.timestamp` 为 null；在线全文称其为最新上游有效时间，无有效数据才为 null。没有逐标的时间字段保证。不得用客户端当前时间冒充报价时间，需实测并确认。[行情契约](https://github.com/HiThink-Tech/Financial-API/blob/main/docs/api/endpoints-prices.md)、[在线全文](https://fuyao.aicubes.cn/llms-full.txt)
- 未在本次查阅的公开 API 文档中确认明确价格表、免费长期额度、数值 QPS、并发上限、刷新间隔或 SLA。不能据此宣传免费、无限调用或比 TickFlow 稳定。公开协议有 `4001` 限流和 `2003` 权限错误，说明权限/配额需要另行核实。[通用契约](https://github.com/HiThink-Tech/Financial-API/blob/main/docs/api/README.md)
- REST 使用 `X-api-key`，响应信封为 `{code,message,request_id,data}`；HTTP 200 后仍须检查 `code == 0`。可直接通过 Java HTTP 客户端接入，不必为了使用扶摇额外启动 Python 或 MCP。[通用契约](https://github.com/HiThink-Tech/Financial-API/blob/main/docs/api/README.md)

## 建议迁移与验收顺序

1. 增加数据源适配策略与配置，先接 A 股日 K、基础报价、指数；维持当前对内响应格式，缺失字段显式处理。保留现有 MACD 算法。
2. 在交易时段对照股票与四大指数：沪深北、ST、停牌、除权样本；核实复权、成交量单位、成交额范围、时间与更新间隔，特别验证两市盘中成交额。
3. 将龙虎榜、同花顺热榜、三类股票池作为第二批，先明确哪些展示字段补数、哪些需要改名或保持缺失。
4. 保留 AKShare 的股东、新闻、行业资金、强势池和赚钱效应；保留现有东方财富资金榜与 Baostock 历史成交额链路，直到对应能力独立验收。
5. 拿到 API Key 后确认功能授权、收费与请求限额，测量成功率和延迟，再决定是否停用 TickFlow。当前未做真实调用，不给出“已可全量替换”的上线结论。

总体建议：**扶摇值得做小范围接入验证，最适合先替代日 K 和基础行情；若目标是一个 API 承担当前全部数据需求，现有公开能力仍不够。**
