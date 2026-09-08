# 同花顺金融数据 API 替换 TickFlow：接入契约

核查日期：2026-09-07。重新读取用户指定站点的[官方完整文档](https://fuyao.aicubes.cn/llms-full.txt)。本笔记只确认公开契约，未使用密钥调用业务接口。当前迁移范围为基础报价、日 K / 本地 MACD、沪深指数当日成交额；AKShare 既有功能继续保留。

## 鉴权与错误处理

- Base URL：`https://fuyao.aicubes.cn`，请求头 `X-api-key: <key>`。所有下述端点都是 GET。
- 标的必须使用完整代码，例如 `600519.SH`、`000001.SZ`；不接受纯六位代码。时间为毫秒 Unix 时间戳，日期解释使用 Asia/Shanghai。
- 响应为 `{code,message,request_id,data}`。文档说明业务错误仍返回 HTTP 200，因此必须同时检查 HTTP 状态和 `code == 0`。
- `2001` 密钥缺失/无效，`2003` 无能力权限，`3002` 数据未就绪，`4001` 限流，`5002` 上游超时，`5003` 上游不可用。文档没有公布具体 QPS、长期免费额度或更新 SLA。

来源：[总览与通用约定](https://fuyao.aicubes.cn/docs/api-reference/overview/)、[价格接口](https://fuyao.aicubes.cn/docs/api-reference/prices/)。

## 基础报价

`GET /api/a-share/prices/snapshot?thscodes=600519.SH,000001.SZ`

显式传 `thscodes` 时按入参顺序取数、不分页；省略时支持 `limit`（默认 100）和 `offset`（默认 0）。迁移项目单股查询使用显式代码。

响应 `data.item[]` 的映射：

| 官方字段 | 项目语义 | 单位或注意事项 |
| --- | --- | --- |
| `thscode` | symbol | 完整市场后缀 |
| `last_price` | 最新价 | 元 |
| `open_price/high_price/low_price/prev_price` | 开/高/低/昨收 | 元 |
| `price_change` | 涨跌额 | 元 |
| `price_change_ratio_pct` | 涨跌幅 | 已是百分数，0.5 就是 0.5%，不得再乘 100 |
| `volume` | 成交量 | 股 |
| `turnover` | 成交额 | 元 |

`data.timestamp` 为这次快照中最新上游有效数据时间，无有效数据时为 null；不是每只股票独立时间。响应没有中文名，应使用项目股票基础资料。没有换手率、普通时段量比、委买委卖、盘口深度；缺失字段保持 null。可由高低与昨收计算振幅，但昨收无效时保持 null。

来源：[行情快照字段](https://fuyao.aicubes.cn/docs/api-reference/prices/)。注意：本次在线文档明确支持有效 `timestamp`，此前 GitHub 文档的显式代码 timestamp 恒 null 描述不应覆盖本次在线契约。

## 日 K 与 MACD

`GET /api/a-share/prices/historical?thscode=600519.SH&interval=1d&start=<ms>&end=<ms>&adjust=forward`

- 一次只接受一个代码，窗口不超过 10 年；`start/end/interval/thscode` 必填。
- 仅支持 `interval=1d`。复权可选 `none/forward/backward`，默认前复权 `forward`；实现应显式传入，避免依赖默认值。
- `offset` 默认 0，但未公开页大小、`limit` 参数、总条数或排序保证。因此不能依赖未经文档证实的固定分页规则。
- 返回 `data.item[]`：`date_ms,open_price,high_price,low_price,close_price,volume,turnover`。量为股、额为元。`data.timestamp` 为最新一根 K 线的上游有效时间。
- 项目的 `count` 需要在本地按日期排序、去重后取最近 N 根；自然日窗口不能等同 N 根交易日。查询窗口应留足休市/停牌空间，必要时扩窗或分时间段，达到 10 年或上市历史不足时允许实际返回不足。
- 公共价格接口未提供 MACD，沿用项目现有收盘价 EMA / MACD 计算，不改变指标算法。

来源：[历史 K 线](https://fuyao.aicubes.cn/docs/api-reference/prices/)、[历史 K 线 MCP 参数](https://fuyao.aicubes.cn/docs/mcp/tools/get_a_share_prices_historical/)。

## 沪深成交额与指数

`GET /api/a-share-index/prices/snapshot?thscodes=000001.SH,399001.SZ`

必须传指数完整代码，`limit/offset` 无效。结构与个股快照相同，使用 `turnover`（元），分别映射沪市/深市金额，并验证两个代码都确实返回有效金额后相加。使用 `data.timestamp` 判断交易日期，不要把旧交易日或无时间的数据标成今天。文档描述时间为批次最新时间，仍无法独立证明两只指数处于同一时刻。

历史指数可用 `GET /api/a-share-index/prices/historical`，必填 `thscode/interval=1d/start/end`，同样最大 10 年，没有 `adjust` 与 `offset` 参数，K 线字段同上。

公开文档给出指数 `turnover` 字段，但未详细证明上证综指和深证成指的金额统计范围就是项目所需的沪深全市场总成交额。需要带密钥对照实际数据验收，不能仅凭字段名宣称口径已验证。

来源：[指数行情及历史接口](https://fuyao.aicubes.cn/docs/api-reference/a-share-index/)。

## 可实现范围与缺口

现有基础报价、日 K / 本地 MACD 和指数成交额调用均有可接入的公开端点，支持在保留项目内部响应结构的前提下移除 TickFlow 调用。文档未列出盘口深度、WebSocket、分钟 K、通用快照换手率/量比；不能虚构替代能力。上线验证仍需用户自行配置有效 API Key，核验授权、真实数据与成交额口径。上述限制不影响保留 AKShare 筹码、榜单和其他未使用 TickFlow 的数据链路。

## 本次实现与验证

- 新增 FUYAO 行情配置，接入基础报价、前复权日 K（保留本地 MACD 算法）及沪深指数快照成交额；旧数据源的 Key 不迁移、不作为 FUYAO Key 使用。
- 配置的 API URL 作为基础地址，Key 通过子进程环境变量传递，再写入请求头。AKShare 筹码等接口、Baostock 历史成交额保持原有来源。
- 内部行情结构保持一致；涨跌幅直接使用百分数，换手率、量比缺失保持 null。K 线时间升序去重，窗口不足时扩展到最多 3650 天，上市历史不足时返回实际条数。
- 验证使用官方契约模拟响应覆盖鉴权、业务错误、单位、排序、缺失时间和成交额字段；没有使用有效 API Key 进行线上业务接口验收。指数成交额口径和实时数据有效性仍需交易时段对照。

## 标的增量同步补充

[官方标的列表文档](https://fuyao.aicubes.cn/docs/api-reference/ticker-list/)规定使用 `GET /api/meta/tickers/list`，传 `asset_type=a-share`，`limit` 最大 10000，递增 `offset` 直到 `item.length < limit`。字段包括 `thscode/ticker/name/exchange/asset_type/currency`，不包括行业、上市日期、拼音或控制人。实现只更新完整代码、纯代码和名称，其余现存资料保留；完整分页获取及校验成功后才执行数据库事务，空列表和请求失败均不写库。
