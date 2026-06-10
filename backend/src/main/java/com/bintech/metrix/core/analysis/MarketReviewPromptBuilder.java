package com.bintech.metrix.core.analysis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MarketReviewPromptBuilder {

    private static final List<String> INDEX_NAMES = List.of("上证指数", "深证成指", "创业板指", "科创50");

    /**
     * 构建大盘复盘 AI 提示词，包含四大指数的实时行情和近30日K线数据
     */
    public String build(Map<String, Object> indexData, String reviewDate) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名20年专业的金融从业分析师。请对").append(reviewDate).append("的A股市场进行大盘复盘分析。\n\n");

        prompt.append("【各大指数今日表现】\n");
        for (String name : INDEX_NAMES) {
            Map<String, Object> idx = (Map<String, Object>) indexData.get(name);
            if (idx == null) continue;
            prompt.append(name).append("：");
            prompt.append("涨跌幅 ").append(idx.get("changePct")).append("%\n");
            Map<String, Object> latest = (Map<String, Object>) idx.get("latest");
            if (latest != null) {
                prompt.append("  收盘价：").append(latest.get("close")).append("\n");
                prompt.append("  开盘价：").append(latest.get("open")).append("\n");
                prompt.append("  最高价：").append(latest.get("high")).append("\n");
                prompt.append("  最低价：").append(latest.get("low")).append("\n");
            }
        }
        prompt.append("\n【各大指数近期K线数据】\n");
        for (String name : INDEX_NAMES) {
            Map<String, Object> idx = (Map<String, Object>) indexData.get(name);
            if (idx == null) continue;
            prompt.append(name).append("（最近30个交易日）：\n");
            prompt.append("日期 | 开盘 | 收盘 | 最高 | 最低 | 成交量\n");
            List<Map<String, Object>> records = (List<Map<String, Object>>) idx.get("records");
            if (records != null) {
                int start = Math.max(0, records.size() - 30);
                for (int i = records.size() - 1; i >= start; i--) {
                    Map<String, Object> r = records.get(i);
                    prompt.append(r.get("date")).append(" | ")
                            .append(r.get("open")).append(" | ")
                            .append(r.get("close")).append(" | ")
                            .append(r.get("high")).append(" | ")
                            .append(r.get("low")).append(" | ")
                            .append(r.get("volume")).append("\n");
                }
            }
            prompt.append("\n");
        }

        prompt.append("请提供以下分析内容：\n");
        prompt.append("1. 大盘走势回顾：总结今日各指数的整体表现，分析主要驱动因素\n");
        prompt.append("2. 板块热点分析：分析今日领涨和领跌板块及原因\n");
        prompt.append("3. 市场情绪研判：基于成交量、涨跌家数等判断市场情绪\n");
        prompt.append("4. 重大事件解读：分析今日影响市场的重大事件或政策\n");
        prompt.append("5. 技术面分析：结合K线形态、均线系统等判断趋势\n");
        prompt.append("6. 资金面分析：分析资金流向和成交量变化\n");
        prompt.append("7. 后市展望：对未来一段时间的市场走势做出判断\n");
        prompt.append("8. 投资策略建议：给出短期和中期的投资策略建议\n");
        prompt.append("请使用中文回答。");
        prompt.append("【提示】直接输出内容即可，请使用Markdown格式输出，适当增加表格和表情符号形式展示，使整体内容展示的更美观和直观。\n\n");
        prompt.append("【重要】最后请在分析内容之后另起一行，用【核心总结】开头输出一段500字以内的核心总结，核心总结需要高度概括市场涨跌分布、行业涨跌排行、成交额、涨停和跌停对比、资金流向、市场情绪、驱动因素和后市展望。" +
                "请使用text格式输出，适当增加表情符号形式展示，使整体内容展示的更美观和直观。" +
                "\n");
        prompt.append("【重要约束】请确认所提供的数据是否为").append(reviewDate).append("的行情数据。如果不是").append(reviewDate).append("的数据，请直接告知用户数据日期不匹配，不要进行分析，不要在回答中输出任何分析内容。");
        return prompt.toString();
    }
}
