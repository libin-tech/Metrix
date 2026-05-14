package com.bin.stockanalysis.core.analysis;

import com.bin.stockanalysis.model.AnalysisOverview;
import com.bin.stockanalysis.model.BattlePlan;
import com.bin.stockanalysis.model.DataPivot;
import com.bin.stockanalysis.model.RealTimeMarket;
import com.bin.stockanalysis.service.AiModelService;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisOverviewBuilder {

    private final AiModelService aiModelService;

    /**
     * 从完整分析报告中提取核心洞察和关联板块
     *
     * @param analysisResult AI分析报告全文
     * @param modelType      AI模型类型
     * @return String
     */
    public String generateCoreInsight(String analysisResult, String modelType) {

        try {
            if (analysisResult == null || analysisResult.isBlank()) {
                log.warn("分析报告为空，跳过核心洞察生成");
                return "暂无核心洞察";
            }
            String prompt = "请将以下股票分析报告总结为400字以内的核心洞察，包括基本面，技术面，情绪面，新闻舆情还有操作建议。帮助投资者快速了解该股票的核心要点和操作。" +
                    "另外，请在核心洞察之后单独列出该股票最相关的3个关联板块（不受字数限制），并标记是否核心热门板块。以表格形式呈现。"+
                    "最后以Markdown格式回复，适当增加一点符号，看起来更美观和直观一些。直接回复内容即可不需要介绍和标题。" +
                    "\n参考完整分析报告：\n" + analysisResult;
            return aiModelService.generateAnalysis(prompt, modelType);
        } catch (Exception e) {
            log.error("生成核心洞察失败: {}", e.getMessage(), e);
            return "核心洞察分析失败";
        }

    }

    /**
     * 从AI回复中解析关联板块列表
     */
    private List<String> parseRelatedSectors(String response) {
        List<String> sectors = new ArrayList<>();
        try {
            int idx = response.indexOf("【关联板块】");
            if (idx < 0) idx = response.indexOf("关联板块");
            if (idx < 0) return sectors;

            String section = response.substring(idx);
            for (String line : section.split("\n")) {
                line = line.trim();
                if (line.startsWith("-") || line.startsWith("*")) {
                    String name = line.replaceAll("^[-*]\\s*", "").replaceAll("\\*\\*", "").trim();
                    if (!name.isEmpty()) {
                        sectors.add(name);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析关联板块失败: {}", e.getMessage());
        }
        return sectors;
    }

    /**
     * 构建分析概览，封装实时行情、数据透视、作战计划
     *
     * @param marketData   实时行情数据
     * @param klinesData   K线数据
     * @param analysisResult AI分析报告
     * @param coreInsight  核心洞察
     * @param chipData     筹码分布数据（AKShare）
     * @return 分析概览对象
     */
    public AnalysisOverview build(Map<String, Object> marketData, Map<String, Object> klinesData,
                                   String analysisResult, String coreInsight, Map<String, Object> chipData) {
        try {
            AnalysisOverview overview = new AnalysisOverview();
            overview.setCoreInsight(coreInsight);
            overview.setRealTimeMarket(buildRealTimeMarket(marketData));
            overview.setDataPivot(buildDataPivot(marketData, klinesData, chipData));
            overview.setBattlePlan(buildBattlePlan(marketData, klinesData));
            return overview;
        } catch (Exception e) {
            log.error("构建分析概览数据失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private RealTimeMarket buildRealTimeMarket(Map<String, Object> marketData) {
        if (marketData == null) return null;
        try {
            JSONObject marketDataJson = new JSONObject(marketData);
            JSONArray data = marketDataJson.getJSONArray("data");
            if (data == null || data.isEmpty()) return null;

            JSONObject quote = data.getJSONObject(0);
            BigDecimal lastPrice = quote.getBigDecimal("last_price", BigDecimal.ZERO);
            BigDecimal prevClose = quote.getBigDecimal("prev_close", BigDecimal.ZERO);
            JSONObject ext = quote.getJSONObject("ext");

            // 涨跌幅优先从 ext 获取（change_pct 为小数，需转为百分比值）
            BigDecimal changePercent = ext.getBigDecimal("change_pct", BigDecimal.ZERO);
            if (changePercent.compareTo(BigDecimal.valueOf(1)) < 0) {
                changePercent = changePercent.multiply(BigDecimal.valueOf(100));
            }


            BigDecimal changeAmount = ext.getBigDecimal("change_amount", BigDecimal.ZERO);


            BigDecimal turnoverRate = ext.getBigDecimal("turnover_rate", BigDecimal.ZERO);
            if (turnoverRate.compareTo(BigDecimal.valueOf(1)) < 0) {
                turnoverRate = turnoverRate.multiply(BigDecimal.valueOf(100));
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
                    .amplitude(ext != null ? ext.getBigDecimal("amplitude", BigDecimal.ZERO) : BigDecimal.ZERO)
                    .updateTime(String.valueOf(quote.getLong("timestamp", 0L)))
                    .build();
        } catch (Exception e) {
            log.error("构建实时行情数据失败: {}", e.getMessage());
            return null;
        }
    }

    private DataPivot buildDataPivot(Map<String, Object> marketData, Map<String, Object> klinesData, Map<String, Object> chipData) {
        DataPivot.DataPivotBuilder builder = DataPivot.builder();

        if (marketData != null) {
            try {
                JSONObject md = new JSONObject(marketData);
                if ("success".equals(md.get("status"))) {
                    JSONArray data = md.getJSONArray("data");
                    if (data != null && !data.isEmpty()) {
                        JSONObject quote = data.getJSONObject(0);
                        builder.currentPrice(quote.getBigDecimal("last_price", BigDecimal.ZERO))
                               .volume(quote.getLong("volume", 0L));
                    }
                }
            } catch (Exception e) {
                log.warn("从市场数据解析当前价失败: {}", e.getMessage());
            }
        }

        if (klinesData != null) {
            try {
                JSONObject kd = new JSONObject(klinesData);
                if ("success".equals(kd.get("status"))) {
                    JSONObject data = kd.getJSONObject("data");
                    if (data != null) {
                        JSONArray closeData = data.getJSONArray("close");
                        JSONArray highData = data.getJSONArray("high");
                        JSONArray lowData = data.getJSONArray("low");

                        if (closeData != null && !closeData.isEmpty()) {
                            builder.ma5(calcMA(closeData, 5));
                            builder.ma20(calcMA(closeData, 20));
                            builder.ma60(calcMA(closeData, 60));
                            builder.supportLevel(calcSupport(lowData));
                            builder.resistanceLevel(calcResistance(highData));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("从K线数据计算均线失败: {}", e.getMessage());
            }
        }
        applyChipData(builder, chipData);
        return builder.build();
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
            if (!"success".equals(cd.get("status"))) {
                log.warn("筹码分布数据状态异常: {}", cd.get("message"));
                builder.chipConcentration(BigDecimal.valueOf(0))
                        .chipDistribution("筹码数据获取失败")
                        .profitRatio(BigDecimal.valueOf(0))
                        .lossRatio(BigDecimal.valueOf(0));
                return;
            }
            JSONObject data = cd.getJSONObject("data");
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

            // 筹码集中度：获利比例越偏离50%，筹码越集中
            BigDecimal deviation = profitRatio.subtract(BigDecimal.valueOf(50)).abs();
            BigDecimal concentration = BigDecimal.valueOf(50).subtract(deviation.multiply(BigDecimal.valueOf(0.8)))
                    .setScale(1, java.math.RoundingMode.HALF_UP);

            String distributionDesc;
            if (profitRatio.compareTo(BigDecimal.valueOf(70)) > 0) {
                distributionDesc = "获利盘占比高，筹码集中在低位区域，上方抛压较轻";
            } else if (profitRatio.compareTo(BigDecimal.valueOf(40)) > 0) {
                distributionDesc = "获利盘与套牢盘分布均衡，筹码博弈激烈";
            } else {
                distributionDesc = "套牢盘占比高，上方存在较大压力位";
            }

            // 生成筹码综合总结
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
            builder.chipConcentration(BigDecimal.valueOf(55.5))
                    .chipDistribution("筹码数据解析失败")
                    .profitRatio(BigDecimal.valueOf(50))
                    .lossRatio(BigDecimal.valueOf(50));
        }
    }

    private String buildChipSummary(BigDecimal avgCost, BigDecimal cost90Low, BigDecimal cost90High,
                                     BigDecimal concentration90, BigDecimal cost70Low, BigDecimal cost70High,
                                     BigDecimal concentration70, BigDecimal profitRatio, BigDecimal lossRatio) {
        StringBuilder sb = new StringBuilder();
        sb.append("平均成本").append(avgCost).append("元；");
        sb.append("90%筹码分布在").append(cost90Low).append("-").append(cost90High)
                .append("元区间（集中度").append(concentration90.multiply(BigDecimal.valueOf(100)).setScale(1, java.math.RoundingMode.HALF_UP)).append("%）；");
        sb.append("70%筹码分布在").append(cost70Low).append("-").append(cost70High)
                .append("元区间（集中度").append(concentration70.multiply(BigDecimal.valueOf(100)).setScale(1, java.math.RoundingMode.HALF_UP)).append("%）；");
        sb.append("获利盘占比").append(profitRatio).append("%，");
        sb.append("套牢盘占比").append(lossRatio).append("%");
        return sb.toString();
    }

    private BattlePlan buildBattlePlan(Map<String, Object> marketData, Map<String, Object> klinesData) {
        BigDecimal currentPrice = BigDecimal.ZERO;
        BigDecimal ma5 = BigDecimal.ZERO;
        BigDecimal ma20 = BigDecimal.ZERO;
        BigDecimal resistance = BigDecimal.ZERO;

        if (marketData != null) {
            try {
                JSONObject md = new JSONObject(marketData);
                if ("success".equals(md.get("status"))) {
                    JSONArray data = md.getJSONArray("data");
                    if (data != null && !data.isEmpty()) {
                        currentPrice = data.getJSONObject(0).getBigDecimal("close", BigDecimal.ZERO);
                    }
                }
            } catch (Exception e) {
                log.warn("获取当前价失败: {}", e.getMessage());
            }
        }

        if (klinesData != null) {
            try {
                JSONObject kd = new JSONObject(klinesData);
                if ("success".equals(kd.get("status"))) {
                    JSONObject data = kd.getJSONObject("data");
                    if (data != null) {
                        JSONArray closeData = data.getJSONArray("close");
                        JSONArray highData = data.getJSONArray("high");
                        if (closeData != null && !closeData.isEmpty()) {
                            ma5 = calcMA(closeData, 5);
                            ma20 = calcMA(closeData, 20);
                            resistance = calcResistance(highData);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取均线数据失败: {}", e.getMessage());
            }
        }

        BigDecimal idealEntry = ma5.multiply(BigDecimal.valueOf(0.99));
        BigDecimal suboptimalEntry = ma20.multiply(BigDecimal.valueOf(0.98));
        BigDecimal stopLoss = ma20.multiply(BigDecimal.valueOf(0.97));
        BigDecimal target = resistance.compareTo(BigDecimal.ZERO) > 0
                ? resistance : currentPrice.multiply(BigDecimal.valueOf(1.10));

        BigDecimal risk = currentPrice.subtract(stopLoss).abs();
        BigDecimal reward = target.subtract(currentPrice);
        BigDecimal riskRewardRatio = risk.compareTo(BigDecimal.ZERO) > 0
                ? reward.divide(risk, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.valueOf(1.2);

        return BattlePlan.builder()
                .idealEntryPrice(idealEntry.setScale(2, java.math.RoundingMode.HALF_UP))
                .idealEntryDesc("回踩MA5支撑且乖离率修复至安全区")
                .suboptimalEntryPrice(suboptimalEntry.setScale(2, java.math.RoundingMode.HALF_UP))
                .suboptimalEntryDesc("回踩MA20强支撑，技术修复更充分")
                .stopLossPrice(stopLoss.setScale(2, java.math.RoundingMode.HALF_UP))
                .stopLossDesc("跌破MA20下方3%，技术形态破位")
                .targetPrice(target.setScale(2, java.math.RoundingMode.HALF_UP))
                .targetDesc("前高压力位，风险回报比约1:1.2")
                .riskRewardRatio(riskRewardRatio)
                .build();
    }

    private BigDecimal calcMA(JSONArray closeData, int period) {
        int count = Math.min(period, closeData.size());
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = closeData.size() - count; i < closeData.size(); i++) {
            sum = sum.add(closeData.getBigDecimal(i));
        }
        return sum.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal calcSupport(JSONArray lowData) {
        if (lowData == null || lowData.size() < 20) return BigDecimal.ZERO;
        BigDecimal support = lowData.getBigDecimal(lowData.size() - 1);
        for (int i = lowData.size() - 20; i < lowData.size(); i++) {
            BigDecimal val = lowData.getBigDecimal(i);
            if (val.compareTo(support) < 0) support = val;
        }
        return support.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal calcResistance(JSONArray highData) {
        if (highData == null || highData.size() < 20) return BigDecimal.ZERO;
        BigDecimal resistance = highData.getBigDecimal(highData.size() - 1);
        for (int i = highData.size() - 20; i < highData.size(); i++) {
            BigDecimal val = highData.getBigDecimal(i);
            if (val.compareTo(resistance) > 0) resistance = val;
        }
        return resistance.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
