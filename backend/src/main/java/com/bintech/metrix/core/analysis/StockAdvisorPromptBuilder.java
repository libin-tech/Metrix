package com.bintech.metrix.core.analysis;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.repository.entity.StockBasic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class StockAdvisorPromptBuilder {

    private static final String SYSTEM_ROLE = "你是一名20年专业的A股金融从业分析师，擅长技术分析、基本面分析和市场情绪研判。";

    private static final String REJECTION_RULE = "仅分析沪深A股个股（暂不支持北交所）、A股相关指数与板块，其他市场、基金等资产和无关任务必须拒答。";

    private static final String OUTPUT_FORMAT = """
            只输出最终分析报告，不输出思考过程或私密推理链。
            用Markdown按“## 结论摘要”“## 分析依据”“## 风险提示”组织，报告不超过2000字。
            保留下方各维度、技术指标表和股东评分规则，缺失数据必须标明，不能推测。
            结尾注明：仅基于公开数据，仅供研究参考，不构成投资建议。
            """;

    private static final String REPORT_STRUCTURE = """
            综合分析报告请包含以下内容：
            
            ### 1、基本档案
            包含：股票名称和代码、最新价格（需注明日期）、市值、PE（给出PE参考是极高还是高还是低）、关联板块（前三，并标记是否为热点板块）、最近前十大股东、股东减持和增持计划、近期分红。
            
            ### 2、行情回顾
            60日K线行情，总结最近关键节点，比如暴跌、涨停、站上20日均线、成交量放量或者缩量，阶段新高和新低。给出日期以及当日的涨跌幅和说明。
            
            ### 3、技术指标解读
            输出指标、数值、信号、解读四列。指标包含：MA5、MA10、MA20、MA60、MACD、RSI6、RSI12、RSI24、成交量、量比。
            #### 3.3.1、均线排列判断：根据技术指标输出均线趋势和总结
            #### 3.3.2、K线形态
            #### 3.3.3、量价关系
            
            ### 4、资金面和消息面
            
            ### 5、综合研判
            
            ### 6、风险提示和客观情景（不提供明确买卖指令）
            """;

    /**
     * 构建完整的聊天提示词
     */
    public String build(String userMessage, StockBasic stockBasic,
                        Map<String, Object> marketData, Map<String, Object> newsData,
                        Map<String, Object> klinesData,
                        Map<String, Object> chipData,
                        Map<String, Object> topFreeShareholdersData) {
        List<String> missing = new ArrayList<>();
        if (!isDataValid(marketData)) missing.add("实时行情");
        if (!isDataValid(newsData)) missing.add("新闻");
        if (!isDataValid(klinesData)) missing.add("K线数据");

        if (!missing.isEmpty()) {
            String missingStr = String.join("、", missing);
            return String.format("""
                    请直接回复以下内容（不要输出思考过程）：
                    
                    "缺少%s数据，为保证回复准确性和专业性，暂时无法提供任何信息。"
                    """, missingStr);
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_ROLE).append("\n\n");
        prompt.append(REJECTION_RULE).append("\n");
        prompt.append(OUTPUT_FORMAT).append("\n");

        prompt.append("用户询问的股票：").append(stockBasic.getName())
                .append("（").append(stockBasic.getSymbol()).append("）\n");
        prompt.append("所属行业：").append(stockBasic.getIndustry() != null ? stockBasic.getIndustry() : "未知").append("\n");
        prompt.append("上市日期：").append(stockBasic.getListDate() != null ? stockBasic.getListDate() : "未知").append("\n\n");

        appendMarketData(prompt, marketData);
        appendNewsData(prompt, newsData);
        appendKlinesData(prompt, klinesData);
        appendChipData(prompt, chipData);
        appendTopFreeShareholdersData(prompt, topFreeShareholdersData);

        prompt.append(REPORT_STRUCTURE).append("\n\n");

        prompt.append("【重要-股东数据分析要求】\n");
        prompt.append("对于十大流通股东数据，请按以下规则分析：\n");
        prompt.append("- 检查股东类型，如果包含机构、社保基金、中央汇金、摩根、高盛等知名机构，标记为\"机构持股较高\"\n");
        prompt.append("- 如果大部分是私募或个人股东，标记为\"个人持股较高\"\n");
        prompt.append("- 机构持股评分：没有机构持股打\"注意风险\"，机构持股占比30%左右打\"及格\"，50%以上标记\"优秀\"\n\n");

        prompt.append("用户问题：").append(userMessage).append("\n");
        prompt.append("请直接输出分析结果，使用中文回答。以Markdown输出。适当增加表情和表格美观一些。");

        return prompt.toString();
    }

    private boolean isDataValid(Map<String, Object> data) {
        if (data == null) return false;
        try {
            Object status = data.get(ApiConstants.KEY_STATUS);
            if (!ApiConstants.STATUS_SUCCESS.equals(status)) return false;
            Object dataObj = data.get("data");
            if (dataObj == null) return false;
            if (dataObj instanceof JSONArray jsonArray) return !jsonArray.isEmpty();
            if (dataObj instanceof JSONObject) return true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void appendNewsData(StringBuilder prompt, Map<String, Object> newsData) {
        if (!isDataValid(newsData)) return;
        try {
            Object dataObj = newsData.get("data");
            if (!(dataObj instanceof JSONArray newsArray) || newsArray.isEmpty()) {
                prompt.append("【相关新闻】\n暂无相关新闻\n\n");
                return;
            }
            prompt.append("【相关新闻】\n");
            for (int i = 0; i < Math.min(newsArray.size(), 10); i++) {
                JSONObject item = newsArray.getJSONObject(i);
                prompt.append("  ").append(i + 1).append(". ");
                prompt.append("标题: ").append(item.getStr(ApiConstants.KEY_TITLE, "未知")).append("\n");
                prompt.append("    摘要: ").append(item.getStr(BusinessConstants.KEY_SUMMARY, "无摘要")).append("\n");
                prompt.append("    来源: ").append(item.getStr(ApiConstants.KEY_SOURCE, "未知来源")).append("\n");
                prompt.append("    时间: ").append(item.getStr("publishTime", "未知时间")).append("\n");
            }
            prompt.append("\n");
        } catch (Exception e) {
            log.warn("解析新闻数据失败: {}", e.getMessage());
        }
    }

    private void appendMarketData(StringBuilder prompt, Map<String, Object> marketData) {
        if (marketData == null) return;
        try {
            JSONObject json = new JSONObject(marketData);
            if (!ApiConstants.STATUS_SUCCESS.equals(json.getStr(ApiConstants.KEY_STATUS))) return;
            JSONArray data = json.getJSONArray("data");
            if (data == null || data.isEmpty()) return;

            JSONObject latestData = data.getJSONObject(0);
            JSONObject ext = latestData.getJSONObject("ext");
            prompt.append("【实时行情】\n");
            prompt.append("最新价: ").append(getValue(latestData, "last_price")).append("\n");
            prompt.append("开盘价: ").append(getValue(latestData, "open")).append("\n");
            prompt.append("最高价: ").append(getValue(latestData, "high")).append("\n");
            prompt.append("最低价: ").append(getValue(latestData, "low")).append("\n");
            prompt.append("成交量: ").append(getValue(latestData, "volume")).append("\n");
            prompt.append("成交额: ").append(getValue(latestData, "amount")).append("\n");
            if (ext != null) {
                prompt.append("涨跌幅: ").append(getValue(ext, "change_pct")).append("%\n");
                prompt.append("涨跌额: ").append(getValue(ext, "change")).append("\n");
                prompt.append("换手率: ").append(getValue(ext, "turnover_rate")).append("%\n");
                prompt.append("市盈率PE: ").append(getValue(ext, "pe")).append("\n");
                prompt.append("总市值: ").append(getValue(ext, "total_mv")).append("\n");
                prompt.append("流通市值: ").append(getValue(ext, "circ_mv")).append("\n");
            }
            prompt.append("\n");
        } catch (Exception e) {
            log.warn("解析行情数据失败: {}", e.getMessage());
        }
    }

    private void appendKlinesData(StringBuilder prompt, Map<String, Object> klinesData) {
        if (klinesData == null) return;
        try {
            JSONObject json = new JSONObject(klinesData);
            if (!ApiConstants.STATUS_SUCCESS.equals(json.getStr(ApiConstants.KEY_STATUS))) return;
            Object dataObj = json.get("data");
            if (!(dataObj instanceof JSONObject klines)) return;

            JSONArray timestampData = klines.getJSONArray("timestamp");
            JSONArray openData = klines.getJSONArray("open");
            JSONArray highData = klines.getJSONArray("high");
            JSONArray lowData = klines.getJSONArray("low");
            JSONArray closeData = klines.getJSONArray("close");
            JSONArray volumeData = klines.getJSONArray("volume");

            if (timestampData == null || timestampData.isEmpty()) return;

            prompt.append("【日K线数据（最近60条）】\n");
            prompt.append("日期 | 开盘 | 最高 | 最低 | 收盘 | 成交量\n");

            int start = Math.max(0, timestampData.size() - 60);
            for (int i = timestampData.size() - 1; i >= start; i--) {
                prompt.append("  ").append(cn.hutool.core.date.DateUtil.format(
                        new java.util.Date(timestampData.getLong(i)), "yyyy-MM-dd"));
                prompt.append(" | ").append(String.format("%.2f", openData.getDouble(i)));
                prompt.append(" | ").append(String.format("%.2f", highData.getDouble(i)));
                prompt.append(" | ").append(String.format("%.2f", lowData.getDouble(i)));
                prompt.append(" | ").append(String.format("%.2f", closeData.getDouble(i)));
                prompt.append(" | ").append(volumeData.getLong(i));
                prompt.append("\n");
            }
            prompt.append("\n");

            JSONObject macdObj = klines.getJSONObject("macd");
            if (macdObj != null) {
                prompt.append("【MACD指标】\n");
                prompt.append("DIF: ").append(macdObj.getBigDecimal("dif", BigDecimal.ZERO)).append("\n");
                prompt.append("DEA: ").append(macdObj.getBigDecimal("dea", BigDecimal.ZERO)).append("\n");
                prompt.append("MACD柱: ").append(macdObj.getBigDecimal("bar", BigDecimal.ZERO)).append("\n");
                prompt.append("信号: ").append(macdObj.getStr("signal", "")).append("\n\n");
            }
        } catch (Exception e) {
            log.warn("解析K线数据失败: {}", e.getMessage());
        }
    }

    private void appendChipData(StringBuilder prompt, Map<String, Object> chipData) {
        if (chipData == null) return;
        try {
            JSONObject cd = new JSONObject(chipData);
            if (!ApiConstants.STATUS_SUCCESS.equals(cd.get(ApiConstants.KEY_STATUS))) {
                prompt.append("【筹码分布】上游不可用，本次缺失，禁止推测筹码指标。\n");
                return;
            }
            JSONObject data = cd.getJSONObject("data");
            if (data == null) return;

            prompt.append("【筹码分布】\n");
            prompt.append("数据日期: ").append(data.getStr("date")).append("\n");
            if (data.getBool("stale", false)) {
                prompt.append("上游不可用，以下为历史缓存，不代表当前筹码。\n");
            }
            prompt.append("获利比例: ").append(data.getBigDecimal("profit_ratio")).append("%\n");
            prompt.append("套牢比例: ").append(data.getBigDecimal("loss_ratio")).append("%\n");
            prompt.append("平均成本: ").append(data.getBigDecimal("avg_cost")).append("\n");
            prompt.append("90%成本区间: ").append(data.getBigDecimal("cost_90_low"))
                    .append(" - ").append(data.getBigDecimal("cost_90_high")).append("\n");
            prompt.append("90%集中度: ").append(data.getBigDecimal("concentration_90")).append("\n");
            prompt.append("70%成本区间: ").append(data.getBigDecimal("cost_70_low"))
                    .append(" - ").append(data.getBigDecimal("cost_70_high")).append("\n");
            prompt.append("70%集中度: ").append(data.getBigDecimal("concentration_70")).append("\n\n");
        } catch (Exception e) {
            log.warn("解析筹码数据失败: {}", e.getMessage());
        }
    }

    private void appendTopFreeShareholdersData(StringBuilder prompt, Map<String, Object> topFreeShareholdersData) {
        if (topFreeShareholdersData == null) return;
        try {
            JSONObject cd = new JSONObject(topFreeShareholdersData);
            if (!ApiConstants.STATUS_SUCCESS.equals(cd.get(ApiConstants.KEY_STATUS))) return;
            JSONArray data = cd.getJSONArray(ApiConstants.KEY_DATA);
            if (data == null || data.isEmpty()) return;

            prompt.append("【十大流通股东】\n");
            int count = Math.min(data.size(), 10);
            for (int i = 0; i < count; i++) {
                JSONObject item = data.getJSONObject(i);
                prompt.append("  ").append(i + 1).append(". ")
                        .append(item.getStr("holder_name", "未知"))
                        .append(" | 类型: ").append(item.getStr("holder_type", "未知"))
                        .append(" | 持股占流通股比: ").append(item.getBigDecimal("free_holdnum_ratio", BigDecimal.ZERO)).append("%\n");
            }
            prompt.append("\n");
        } catch (Exception e) {
            log.warn("解析十大流通股东数据失败: {}", e.getMessage());
        }
    }

    private String getValue(JSONObject json, String key) {
        try {
            Object value = json.get(key);
            return value != null ? value.toString() : "未知";
        } catch (Exception e) {
            return "未知";
        }
    }
}
