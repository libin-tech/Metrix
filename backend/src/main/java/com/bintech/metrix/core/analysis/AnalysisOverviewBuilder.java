package com.bintech.metrix.core.analysis;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.model.AnalysisOverview;
import com.bintech.metrix.model.BattlePlan;
import com.bintech.metrix.model.DataPivot;
import com.bintech.metrix.model.RealTimeMarket;
import com.bintech.metrix.service.AiModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisOverviewBuilder {

    private final AiModelService aiModelService;
    private final CoreInsightPromptBuilder coreInsightPromptBuilder;

    /**
     * 从完整分析报告中提取核心洞察和关联板块（无用户ID版本）
     */
    public String generateCoreInsight(String analysisResult, String modelType) {
        return generateCoreInsight(analysisResult, modelType, null);
    }

    /**
     * 从完整分析报告中提取核心洞察和关联板块
     *
     * @param analysisResult AI分析报告全文
     * @param modelType      AI模型类型
     * @param userId         用户ID
     * @return String
     */
    public String generateCoreInsight(String analysisResult, String modelType, Long userId) {

        try {
            if (analysisResult == null || analysisResult.isBlank()) {
                log.warn("分析报告为空，跳过核心洞察生成");
                return "暂无核心洞察";
            }
            String prompt = coreInsightPromptBuilder.buildCoreInsightPrompt(analysisResult);
            return aiModelService.generateAnalysis(prompt, modelType, userId);
        } catch (Exception e) {
            log.error("生成核心洞察失败: {}", e.getMessage(), e);
            return "核心洞察分析失败";
        }

    }

    /**
     * 构建完整的分析概览对象，包含实时行情、数据枢纽、作战计划和筹码分析
     */
    public AnalysisOverview build(Map<String, Object> marketData, Map<String, Object> klinesData,
                                   String analysisResult, String coreInsight, Map<String, Object> chipData,
                                   Map<String, Object> topFreeShareholdersData) {
        try {
            AnalysisOverview overview = new AnalysisOverview();
            overview.setCoreInsight(coreInsight);
            overview.setRealTimeMarket(buildRealTimeMarket(marketData));
            overview.setDataPivot(buildDataPivot(marketData, klinesData, chipData));
            overview.setBattlePlan(buildBattlePlan(marketData, klinesData));
            overview.setTopFreeShareholdersData(topFreeShareholdersData != null ? JSONUtil.toJsonStr(topFreeShareholdersData) : null);
            overview.setTopFreeShareholdersAnalysis(buildTopFreeShareholdersAnalysis(topFreeShareholdersData));
            return overview;
        } catch (Exception e) {
            log.error("构建分析概览数据失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从市场数据中解析实时行情（当前价、涨跌幅、成交量等）
     */
    private RealTimeMarket buildRealTimeMarket(Map<String, Object> marketData) {
        if (marketData == null) return null;
        try {
            JSONObject marketDataJson = new JSONObject(marketData);
            JSONArray data = marketDataJson.getJSONArray(ApiConstants.KEY_DATA);
            if (data == null || data.isEmpty()) return null;

            JSONObject quote = data.getJSONObject(0);
            BigDecimal lastPrice = quote.getBigDecimal("last_price", BigDecimal.ZERO);
            BigDecimal prevClose = quote.getBigDecimal("prev_close", BigDecimal.ZERO);
            JSONObject ext = quote.getJSONObject("ext");

            BigDecimal changePercent = ext.getBigDecimal("change_pct", BigDecimal.ZERO);
            if (changePercent.compareTo(BusinessConstants.CHANGE_PCT_THRESHOLD) < 0) {
                changePercent = changePercent.multiply(BusinessConstants.PCT_MULTIPLIER);
            }

            BigDecimal changeAmount = ext.getBigDecimal("change_amount", BigDecimal.ZERO);

            BigDecimal turnoverRate = ext.getBigDecimal("turnover_rate", BigDecimal.ZERO);
            if (turnoverRate.compareTo(BusinessConstants.TURNOVER_RATE_THRESHOLD) < 0) {
                turnoverRate = turnoverRate.multiply(BusinessConstants.PCT_MULTIPLIER);
            }

            return RealTimeMarket.builder()
                    .changePercent(changePercent)
                    .currentPrice(lastPrice)
                    .changeAmount(changeAmount)
                    .openPrice(quote.getBigDecimal("open", BigDecimal.ZERO))
                    .highPrice(quote.getBigDecimal("high", BigDecimal.ZERO))
                    .lowPrice(quote.getBigDecimal("low", BigDecimal.ZERO))
                    .prevClosePrice(prevClose)
                    .volume(quote.getLong("volume", 0L))
                    .turnover(quote.getBigDecimal("amount", BigDecimal.ZERO))
                    .turnoverRate(turnoverRate)
                    .amplitude(ext.getBigDecimal("amplitude", BigDecimal.ZERO))
                    .updateTime(String.valueOf(quote.getLong("timestamp", 0L)))
                    .build();
        } catch (Exception e) {
            log.error("构建实时行情数据失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建数据枢纽：整合均线、MACD、支撑/阻力位、筹码分布
     */
    private DataPivot buildDataPivot(Map<String, Object> marketData, Map<String, Object> klinesData, Map<String, Object> chipData) {
        DataPivot.DataPivotBuilder builder = DataPivot.builder();

        if (marketData != null) {
            parseMarketDataForPivot(builder, marketData);
        }

        if (klinesData != null) {
            parseKlinesForPivot(builder, klinesData);
        }
        applyChipData(builder, chipData);
        return builder.build();
    }

    private void parseMarketDataForPivot(DataPivot.DataPivotBuilder builder, Map<String, Object> marketData) {
        try {
            JSONObject md = new JSONObject(marketData);
            if (!ApiConstants.STATUS_SUCCESS.equals(md.get(ApiConstants.KEY_STATUS))) {
                return;
            }
            JSONArray data = md.getJSONArray(ApiConstants.KEY_DATA);
            if (data == null || data.isEmpty()) {
                return;
            }
            JSONObject quote = data.getJSONObject(0);
            builder.currentPrice(quote.getBigDecimal("last_price", BigDecimal.ZERO))
                   .volume(quote.getLong("volume", 0L));
        } catch (Exception e) {
            log.warn("从市场数据解析当前价失败: {}", e.getMessage());
        }
    }

    private void parseKlinesForPivot(DataPivot.DataPivotBuilder builder, Map<String, Object> klinesData) {
        try {
            JSONObject kd = new JSONObject(klinesData);
            if (!ApiConstants.STATUS_SUCCESS.equals(kd.get(ApiConstants.KEY_STATUS))) {
                return;
            }
            JSONObject data = kd.getJSONObject(ApiConstants.KEY_DATA);
            if (data == null) {
                return;
            }
            JSONArray closeData = data.getJSONArray("close");
            JSONArray highData = data.getJSONArray("high");
            JSONArray lowData = data.getJSONArray("low");
            if (closeData == null || closeData.isEmpty()) {
                return;
            }
            builder.ma5(calcMA(closeData, BusinessConstants.MA_PERIOD_5));
            builder.ma20(calcMA(closeData, BusinessConstants.MA_PERIOD_20));
            builder.ma60(calcMA(closeData, BusinessConstants.MA_PERIOD_60));
            builder.supportLevel(calcSupport(lowData));
            builder.resistanceLevel(calcResistance(highData));

            JSONObject macdData = data.getJSONObject("macd");
            if (macdData != null) {
                builder.macdDif(macdData.getBigDecimal("dif", BigDecimal.ZERO));
                builder.macdDea(macdData.getBigDecimal("dea", BigDecimal.ZERO));
                builder.macdBar(macdData.getBigDecimal("bar", BigDecimal.ZERO));
                builder.macdSignal(macdData.getStr("signal", ""));
            }
        } catch (Exception e) {
            log.warn("从K线数据计算均线/MACD失败: {}", e.getMessage());
        }
    }

    private void applyChipData(DataPivot.DataPivotBuilder builder, Map<String, Object> chipData) {
        if (chipData == null) {
            builder.chipConcentration(BigDecimal.valueOf(0))
                    .chipDistribution("暂无筹码数据")
                    .profitRatio(BigDecimal.valueOf(0))
                    .lossRatio(BigDecimal.valueOf(0));
            return;
        }
        try {
            JSONObject cd = new JSONObject(chipData);
            if (!ApiConstants.STATUS_SUCCESS.equals(cd.get(ApiConstants.KEY_STATUS))) {
                log.warn("筹码分布数据状态异常: {}", cd.get("message"));
                builder.chipConcentration(BigDecimal.valueOf(0))
                        .chipDistribution("筹码数据获取失败")
                        .profitRatio(BigDecimal.valueOf(0))
                        .lossRatio(BigDecimal.valueOf(0));
                return;
            }
            JSONObject data = cd.getJSONObject(ApiConstants.KEY_DATA);
            if (data == null) return;

            BigDecimal profitRatio = data.getBigDecimal("profit_ratio");
            BigDecimal lossRatio = data.getBigDecimal("loss_ratio");
            BigDecimal avgCost = data.getBigDecimal("avg_cost");
            BigDecimal cost90Low = data.getBigDecimal("cost_90_low");
            BigDecimal cost90High = data.getBigDecimal("cost_90_high");
            BigDecimal concentration90 = data.getBigDecimal("concentration_90");
            BigDecimal cost70Low = data.getBigDecimal("cost_70_low");
            BigDecimal cost70High = data.getBigDecimal("cost_70_high");
            BigDecimal concentration70 = data.getBigDecimal("concentration_70");

            BigDecimal deviation = profitRatio.subtract(BusinessConstants.CHIP_BALANCE_PERCENT).abs();
            BigDecimal concentration = BusinessConstants.CHIP_BASE_SCORE.subtract(deviation.multiply(BusinessConstants.CHIP_CONCENTRATION_FACTOR))
                    .setScale(1, java.math.RoundingMode.HALF_UP);

            String distributionDesc;
            if (profitRatio.compareTo(BusinessConstants.PROFIT_HIGH_THRESHOLD) > 0) {
                distributionDesc = "获利盘占比高，筹码集中在低位区域，上方抛压较轻";
            } else if (profitRatio.compareTo(BusinessConstants.PROFIT_MID_THRESHOLD) > 0) {
                distributionDesc = "获利盘与套牢盘分布均衡，筹码博弈激烈";
            } else {
                distributionDesc = "套牢盘占比高，上方存在较大压力位";
            }

            String chipSummary = buildChipSummary(avgCost, cost90Low, cost90High, concentration90,
                    cost70Low, cost70High, concentration70, profitRatio, lossRatio);

            builder.chipConcentration(concentration)
                    .chipDistribution(distributionDesc)
                    .profitRatio(profitRatio)
                    .lossRatio(lossRatio)
                    .avgCostPrice(avgCost)
                    .cost90Low(cost90Low)
                    .cost90High(cost90High)
                    .concentration90(concentration90)
                    .cost70Low(cost70Low)
                    .cost70High(cost70High)
                    .concentration70(concentration70)
                    .chipSummary(chipSummary);

            log.info("筹码分布: 集中度={}%, 获利盘={}%, 套牢盘={}%, 平均成本={}, 90%区间={}-{}, 70%区间={}-{}",
                    concentration, profitRatio, lossRatio, avgCost, cost90Low, cost90High, cost70Low, cost70High);
        } catch (Exception e) {
            log.warn("解析筹码分布数据失败: {}", e.getMessage());
            builder.chipConcentration(BusinessConstants.DEFAULT_CHIP_CONCENTRATION)
                    .chipDistribution("筹码数据解析失败")
                    .profitRatio(BusinessConstants.DEFAULT_PROFIT_RATIO)
                    .lossRatio(BusinessConstants.DEFAULT_LOSS_RATIO);
        }
    }

    private String buildChipSummary(BigDecimal avgCost, BigDecimal cost90Low, BigDecimal cost90High,
                                     BigDecimal concentration90, BigDecimal cost70Low, BigDecimal cost70High,
                                     BigDecimal concentration70, BigDecimal profitRatio, BigDecimal lossRatio) {
        StringBuilder sb = new StringBuilder();
        sb.append("平均成本").append(avgCost).append("元；");
        sb.append("90%筹码分布在").append(cost90Low).append("-").append(cost90High)
                .append("元区间（集中度").append(concentration90.multiply(BusinessConstants.PCT_MULTIPLIER).setScale(1, java.math.RoundingMode.HALF_UP)).append("%）；");
        sb.append("70%筹码分布在").append(cost70Low).append("-").append(cost70High)
                .append("元区间（集中度").append(concentration70.multiply(BusinessConstants.PCT_MULTIPLIER).setScale(1, java.math.RoundingMode.HALF_UP)).append("%）；");
        sb.append("获利盘占比").append(profitRatio).append("%，");
        sb.append("套牢盘占比").append(lossRatio).append("%");
        return sb.toString();
    }

    /**
     * 构建作战计划：基于均线计算理想/次优入场价、止损价、目标价和盈亏比
     */
    private BattlePlan buildBattlePlan(Map<String, Object> marketData, Map<String, Object> klinesData) {
        BigDecimal currentPrice = BigDecimal.ZERO;
        BigDecimal ma5 = BigDecimal.ZERO;
        BigDecimal ma20 = BigDecimal.ZERO;
        BigDecimal resistance = BigDecimal.ZERO;

        if (marketData != null) {
            currentPrice = parseBattleCurrentPrice(marketData);
        }

        if (klinesData != null) {
            JSONObject kd = new JSONObject(klinesData);
            ma5 = parseBattleMa(kd, BusinessConstants.MA_PERIOD_5);
            ma20 = parseBattleMa(kd, BusinessConstants.MA_PERIOD_20);
            resistance = parseBattleResistance(kd);
        }

        BigDecimal idealEntry = ma5.multiply(BusinessConstants.IDEAL_ENTRY_FACTOR);
        BigDecimal suboptimalEntry = ma20.multiply(BusinessConstants.SUBOPTIMAL_ENTRY_FACTOR);
        BigDecimal stopLoss = ma20.multiply(BusinessConstants.STOP_LOSS_FACTOR);
        BigDecimal target = resistance.compareTo(BigDecimal.ZERO) > 0
                ? resistance : currentPrice.multiply(BusinessConstants.TARGET_PRICE_FACTOR);

        BigDecimal risk = currentPrice.subtract(stopLoss).abs();
        BigDecimal reward = target.subtract(currentPrice);
        BigDecimal riskRewardRatio = risk.compareTo(BigDecimal.ZERO) > 0
                ? reward.divide(risk, 2, java.math.RoundingMode.HALF_UP)
                : BusinessConstants.DEFAULT_RISK_REWARD_RATIO;

        return BattlePlan.builder()
                .idealEntryPrice(idealEntry.setScale(2, java.math.RoundingMode.HALF_UP))
                .idealEntryDesc(BusinessConstants.IDEAL_ENTRY_DESC)
                .suboptimalEntryPrice(suboptimalEntry.setScale(2, java.math.RoundingMode.HALF_UP))
                .suboptimalEntryDesc(BusinessConstants.SUBOPTIMAL_ENTRY_DESC)
                .stopLossPrice(stopLoss.setScale(2, java.math.RoundingMode.HALF_UP))
                .stopLossDesc(BusinessConstants.STOP_LOSS_DESC)
                .targetPrice(target.setScale(2, java.math.RoundingMode.HALF_UP))
                .targetDesc(BusinessConstants.TARGET_DESC)
                .riskRewardRatio(riskRewardRatio)
                .build();
    }

    private BigDecimal parseBattleCurrentPrice(Map<String, Object> marketData) {
        try {
            JSONObject md = new JSONObject(marketData);
            if (!ApiConstants.STATUS_SUCCESS.equals(md.get(ApiConstants.KEY_STATUS))) {
                return BigDecimal.ZERO;
            }
            JSONArray data = md.getJSONArray(ApiConstants.KEY_DATA);
            if (data == null || data.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return data.getJSONObject(0).getBigDecimal("close", BigDecimal.ZERO);
        } catch (Exception e) {
            log.warn("获取当前价失败: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal parseBattleMa(JSONObject kd, int period) {
        try {
            JSONObject data = kd.getJSONObject(ApiConstants.KEY_DATA);
            if (data == null) return BigDecimal.ZERO;
            JSONArray closeData = data.getJSONArray("close");
            if (closeData == null || closeData.isEmpty()) return BigDecimal.ZERO;
            return calcMA(closeData, period);
        } catch (Exception e) {
            log.warn("获取MA{}失败: {}", period, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal parseBattleResistance(JSONObject kd) {
        try {
            JSONObject data = kd.getJSONObject(ApiConstants.KEY_DATA);
            if (data == null) return BigDecimal.ZERO;
            JSONArray highData = data.getJSONArray("high");
            if (highData == null || highData.isEmpty()) return BigDecimal.ZERO;
            return calcResistance(highData);
        } catch (Exception e) {
            log.warn("获取阻力位失败: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算移动平均线（MA）
     */
    private BigDecimal calcMA(JSONArray closeData, int period) {
        int count = Math.min(period, closeData.size());
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = closeData.size() - count; i < closeData.size(); i++) {
            sum = sum.add(closeData.getBigDecimal(i));
        }
        return sum.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 计算支撑位：取最近 N 日最低价的最小值
     */
    private BigDecimal calcSupport(JSONArray lowData) {
        if (lowData == null || lowData.size() < BusinessConstants.SUPPORT_LOOKBACK) return BigDecimal.ZERO;
        BigDecimal support = lowData.getBigDecimal(lowData.size() - 1);
        for (int i = lowData.size() - BusinessConstants.SUPPORT_LOOKBACK; i < lowData.size(); i++) {
            BigDecimal val = lowData.getBigDecimal(i);
            if (val.compareTo(support) < 0) support = val;
        }
        return support.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 计算阻力位：取最近 N 日最高价的最大值
     */
    private BigDecimal calcResistance(JSONArray highData) {
        if (highData == null || highData.size() < BusinessConstants.SUPPORT_LOOKBACK) return BigDecimal.ZERO;
        BigDecimal resistance = highData.getBigDecimal(highData.size() - 1);
        for (int i = highData.size() - BusinessConstants.SUPPORT_LOOKBACK; i < highData.size(); i++) {
            BigDecimal val = highData.getBigDecimal(i);
            if (val.compareTo(resistance) > 0) resistance = val;
        }
        return resistance.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 分析十大流通股东：统计机构/个人比例，识别知名机构并给出评分
     */
    private String buildTopFreeShareholdersAnalysis(Map<String, Object> topFreeShareholdersData) {
        if (topFreeShareholdersData == null) return "暂无十大流通股东数据";
        try {
            JSONObject cd = new JSONObject(topFreeShareholdersData);
            if (!ApiConstants.STATUS_SUCCESS.equals(cd.get(ApiConstants.KEY_STATUS))) return "十大流通股东数据获取失败";
            JSONArray data = cd.getJSONArray(ApiConstants.KEY_DATA);
            if (data == null || data.isEmpty()) return "暂无十大流通股东数据";

            double totalInstitutionalRatio = 0;
            int institutionalCount = 0;
            int personalCount = 0;
            boolean hasSshefund = false;
            boolean hasCentralHuijin = false;
            boolean hasMorgan = false;
            boolean hasGoldman = false;

            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);
                String holderType = item.getStr("holder_type", "");
                String holderName = item.getStr("holder_name", "");
                double ratio = item.getDouble("free_holdnum_ratio", 0.0);

                if ("个人".equals(holderType)) {
                    personalCount++;
                } else {
                    institutionalCount++;
                    totalInstitutionalRatio += ratio;
                }

                String name = holderName.toLowerCase();
                if (name.contains("社保")) hasSshefund = true;
                if (name.contains("中央汇金") || name.contains("汇金")) hasCentralHuijin = true;
                if (name.contains("摩根")) hasMorgan = true;
                if (name.contains("高盛")) hasGoldman = true;
            }

            double institutionalRatio = data.size() > 0 ? (double) institutionalCount / data.size() * 100 : 0;

            StringBuilder sb = new StringBuilder();
            sb.append("十大流通股东中，机构股东").append(institutionalCount).append("家，个人股东").append(personalCount).append("家；");

            if (hasSshefund || hasCentralHuijin || hasMorgan || hasGoldman) {
                sb.append("包含知名机构（");
                if (hasSshefund) sb.append("社保基金、");
                if (hasCentralHuijin) sb.append("中央汇金、");
                if (hasMorgan) sb.append("摩根、");
                if (hasGoldman) sb.append("高盛、");
                sb.deleteCharAt(sb.length() - 1);
                sb.append("），机构持股占比高；");
            }

            if (institutionalRatio >= 50) {
                sb.append("评分：优秀（机构持股占比超50%）");
            } else if (institutionalRatio >= 30) {
                sb.append("评分：及格（机构持股占比30%左右）");
            } else {
                sb.append("评分：注意风险（机构持股占比不足30%）");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("解析十大流通股东分析失败: {}", e.getMessage());
            return "十大流通股东数据解析失败";
        }
    }
}
