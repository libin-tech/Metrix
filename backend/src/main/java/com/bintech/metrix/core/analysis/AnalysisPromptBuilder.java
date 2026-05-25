package com.bintech.metrix.core.analysis;

import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.repository.entity.StockBasic;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class AnalysisPromptBuilder {

    /**
     * 构建AI分析提示词，整合行情、深度、K线、舆情、筹码、股东数据为完整上下文
     */
    public String build(StockBasic stockBasic, String analysisType,
                        Map<String, Object> marketData, Map<String, Object> depthData,
                        Map<String, Object> klinesData, Map<String, Object> newsSummary,
                        Map<String, Object> chipData,
                        Map<String, Object> topFreeShareholdersData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名20年专业的金融从业分析师。");
        prompt.append("现在，你需要对").append(stockBasic.getName()).append("(").append(stockBasic.getSymbol()).append(")进行").append(analysisType).append("分析。");

        appendMarketData(prompt, marketData);
        appendDepthData(prompt, depthData);
        appendKlinesData(prompt, klinesData);
        appendChipData(prompt, chipData);
        appendTopFreeShareholdersData(prompt, topFreeShareholdersData);
        appendNewsSummary(prompt, newsSummary);

        prompt.append("请提供以下分析内容：\n");
        prompt.append("1. 概述：请对股票进行概述\n");
        prompt.append("2. 关联板块: 最相符的三个板块，如果是当下热门板块进行红色标记，非热门的不标记\n");
        prompt.append("3. 重要信息速览: 舆情情绪、业绩预期、风险警报、利好催化、最新动态概要，并结合十大流通股东数据分析\n");
        prompt.append("4. 核心结论：给出当下的操作建议是买入、卖出、持有、观望四种结论，对于空仓者和持仓者的建议。\n");
        prompt.append("5. 数据透视：当日行情，均线值、均线排列情况、趋势强度、成交量、筹码集中度\n");
        prompt.append("6. 操作建议：给出理想买入区间，支撑位和压力位（结合K线形态和成交量）\n");
        prompt.append("7. 仓位建议：给出仓位建议，建议仓位大小、仓位类型（多仓、空仓、多空）\n");
        prompt.append("8. 基本面分析（包含十大流通股东分析）\n");
        prompt.append("9. 技术面分析（结合K线形态、MACD指标和成交量）\n");
        prompt.append("10. 市场情绪分析（结合五档行情和资金流向）\n");
        prompt.append("11. 投资建议\n");
        prompt.append("12. 风险提示\n");
        prompt.append("13. 走势预测: 给出一周内、一月内的股票走势\n");

        prompt.append("【重要-股东数据分析要求】\n");
        prompt.append("对于十大流通股东数据，请按以下规则分析：\n");
        prompt.append("- 检查股东类型，如果包含机构、社保基金、中央汇金、摩根、高盛等知名机构，标记为\"机构持股较高\"\n");
        prompt.append("- 如果大部分是私募或个人股东，标记为\"个人持股较高\"\n");
        prompt.append("- 机构持股评分：没有机构持股打\"注意风险\"，机构持股占比30%左右打\"及格\"，50%以上标记\"优秀\"\n\n");

        prompt.append("请使用中文回答，直接输出内容即可，并使用Markdown格式输出，适当增加表格形式展示，格式上适当填充一些符号做标记，使整体内容展示的更美观和直观。");

        return prompt.toString();
    }

    private void appendMarketData(StringBuilder prompt, Map<String, Object> marketData) {
        if (marketData == null) return;

        JSONObject marketDataJson = new JSONObject(marketData);
        log.info("市场数据: {}", JSONUtil.toJsonStr(marketDataJson));

        try {
            if (!ApiConstants.STATUS_SUCCESS.equals(marketDataJson.get(ApiConstants.KEY_STATUS))) return;
            JSONArray data = marketDataJson.getJSONArray("data");
            if (data == null || data.isEmpty()) return;

            JSONObject latestData = data.getJSONObject(0);
            JSONObject ext = latestData.getJSONObject("ext");
            prompt.append("【实时行情】\n");
            prompt.append("最新价: ").append(getBigDecimalOrUnknown(latestData, "last_price")).append("\n");
            prompt.append("开盘价: ").append(getBigDecimalOrUnknown(latestData, "open")).append("\n");
            prompt.append("最高价: ").append(getBigDecimalOrUnknown(latestData, "high")).append("\n");
            prompt.append("最低价: ").append(getBigDecimalOrUnknown(latestData, "low")).append("\n");
            prompt.append("成交量: ").append(getLongOrUnknown(latestData, "volume")).append("\n");
            prompt.append("涨跌幅: ").append(ext != null ? getBigDecimalOrUnknown(ext, "change_pct") : "未知").append("%\n\n");
        } catch (Exception e) {
            log.error("解析市场数据失败: {}", e.getMessage(), e);
        }
    }

    private void appendDepthData(StringBuilder prompt, Map<String, Object> depthData) {
        if (depthData == null) return;

        JSONObject depthDataJson = new JSONObject(depthData);
        log.info("深度数据: {}", JSONUtil.toJsonStr(depthDataJson));

        try {
            if (!ApiConstants.STATUS_SUCCESS.equals(depthDataJson.get(ApiConstants.KEY_STATUS))) return;
            Object dataObj = depthDataJson.get("data");
            if (!(dataObj instanceof JSONObject data)) return;

            prompt.append("【市场深度-五档行情】\n");
            JSONArray askPrices = data.getJSONArray("ask_prices");
            JSONArray askVolumes = data.getJSONArray("ask_volumes");
            if (askPrices != null && askVolumes != null && !askPrices.isEmpty()) {
                prompt.append("卖盘（按价格从高到低）：\n");
                int count = Math.min(askPrices.size(), SystemConstants.DEPTH_MAX_LEVELS);
                for (int i = 0; i < count; i++) {
                    prompt.append(String.format("  档%d: 价格=%.2f, 数量=%d\n",
                            i + 1, askPrices.getDouble(i, 0.0), askVolumes.getLong(i, 0L)));
                }
            } else {
                appendDepthLegacy(prompt, data, "asks", "卖盘");
            }

            JSONArray bidPrices = data.getJSONArray("bid_prices");
            JSONArray bidVolumes = data.getJSONArray("bid_volumes");
            if (bidPrices != null && bidVolumes != null && !bidPrices.isEmpty()) {
                prompt.append("买盘（按价格从高到低）：\n");
                int count = Math.min(bidPrices.size(), SystemConstants.DEPTH_MAX_LEVELS);
                for (int i = 0; i < count; i++) {
                    prompt.append(String.format("  档%d: 价格=%.2f, 数量=%d\n",
                            i + 1, bidPrices.getDouble(i, 0.0), bidVolumes.getLong(i, 0L)));
                }
            } else {
                appendDepthLegacy(prompt, data, "bids", "买盘");
            }
            prompt.append("\n");
        } catch (Exception e) {
            log.error("解析深度数据失败: {}", e.getMessage(), e);
        }
    }

    private void appendDepthLegacy(StringBuilder prompt, JSONObject data, String arrayKey, String label) {
        JSONArray items = data.getJSONArray(arrayKey);
        if (items == null || items.isEmpty()) {
            prompt.append(label + "：无数据\n");
            return;
        }
        prompt.append(String.format("%s（按价格从高到低）：\n", label));
        int count = Math.min(items.size(), SystemConstants.DEPTH_MAX_LEVELS);
        for (int i = 0; i < count; i++) {
            JSONObject item = items.getJSONObject(i);
            prompt.append(String.format("  档%d: 价格=%.2f, 数量=%d\n",
                    i + 1, item.getDouble("price", 0.0), item.getLong("size", 0L)));
        }
    }

    private void appendKlinesData(StringBuilder prompt, Map<String, Object> klinesData) {
        if (klinesData == null) return;

        JSONObject klinesDataJson = new JSONObject(klinesData);
        log.info("K线数据: {}", JSONUtil.toJsonStr(klinesDataJson));

        try {
            if (!ApiConstants.STATUS_SUCCESS.equals(klinesDataJson.get(ApiConstants.KEY_STATUS))) return;
            Object dataObj = klinesDataJson.get("data");
            if (!(dataObj instanceof JSONObject klines)) return;

            JSONArray timestampData = klines.getJSONArray("timestamp");
            JSONArray openData = klines.getJSONArray("open");
            JSONArray highData = klines.getJSONArray("high");
            JSONArray lowData = klines.getJSONArray("low");
            JSONArray closeData = klines.getJSONArray("close");
            JSONArray volumeData = klines.getJSONArray("volume");
            JSONArray amountData = klines.getJSONArray("amount");

            prompt.append("【K线数据（日线，最近").append(timestampData.size()).append("条，含预计算MACD指标）】\n");
            prompt.append("格式：日期 | 开盘价 | 最高价 | 最低价 | 收盘价 | 成交量 | 成交额\n");

            int startIndex = Math.max(0, timestampData.size() - SystemConstants.KLINE_DISPLAY_COUNT);
            for (int i = timestampData.size() - 1; i >= startIndex; i--) {
                prompt.append(String.format("  %s | %.2f | %.2f | %.2f | %.2f | %d | %d\n",
                        DateUtil.format(new Date(timestampData.getLong(i)), "yyyy-MM-dd"),
                        openData.getDouble(i), highData.getDouble(i), lowData.getDouble(i),
                        closeData.getDouble(i), volumeData.getLong(i), amountData.getLong(i)));
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
            log.error("解析K线数据失败: {}", e.getMessage(), e);
        }
    }

    private void appendChipData(StringBuilder prompt, Map<String, Object> chipData) {
        if (chipData == null) return;
        try {
            JSONObject cd = new JSONObject(chipData);
            if (!ApiConstants.STATUS_SUCCESS.equals(cd.get(ApiConstants.KEY_STATUS))) return;
            JSONObject data = cd.getJSONObject("data");
            if (data == null) return;

            prompt.append("【筹码分布】\n");
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

    private void appendNewsSummary(StringBuilder prompt, Map<String, Object> newsSummary) {
        if (newsSummary == null) return;
        try {
            Object summaryObj = newsSummary.get("summary");
            if (summaryObj == null) return;
            prompt.append("【新闻舆情摘要】\n");
            prompt.append(summaryObj).append("\n\n");
        } catch (Exception e) {
            log.error("解析新闻摘要失败: {}", e.getMessage(), e);
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
                        .append(" | 持股占流通股比: ").append(item.getBigDecimal("free_holdnum_ratio", BigDecimal.ZERO)).append("%")
                        .append(" | 变动: ").append(item.getStr("change_num", "0")).append("股")
                        .append(" | 变动比率: ").append(item.getBigDecimal("change_ratio", BigDecimal.ZERO)).append("%").append("\n");
            }
            prompt.append("\n");
        } catch (Exception e) {
            log.warn("解析十大流通股东数据失败: {}", e.getMessage());
        }
    }

    private String getBigDecimalOrUnknown(JSONObject json, String key) {
        try {
            Object value = json.get(key);
            return value != null ? value.toString() : "未知";
        } catch (Exception e) {
            return "未知";
        }
    }

    private String getLongOrUnknown(JSONObject json, String key) {
        try {
            Object value = json.get(key);
            return value != null ? value.toString() : "未知";
        } catch (Exception e) {
            return "未知";
        }
    }
}
