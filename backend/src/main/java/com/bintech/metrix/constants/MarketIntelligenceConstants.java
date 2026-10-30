package com.bintech.metrix.constants;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Map;

/** 可解释研究规则的阈值；规则标签不代表收益预测或概率。 */
public final class MarketIntelligenceConstants {
    private MarketIntelligenceConstants() {}
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    public static final Duration REFRESH_INTERVAL = Duration.ofMinutes(5);
    public static final Duration STALE_AFTER = Duration.ofMinutes(15);
    public static final Duration AI_TIMEOUT = Duration.ofSeconds(20);
    public static final Duration RETRY_INTERVAL = Duration.ofSeconds(30);
    public static final String ARCHIVE_CRON = "0 5,15,25,35,45,55 15 * * MON-FRI";
    public static final String MARKET_TIME_ZONE = "Asia/Shanghai";
    public static final int DEFAULT_INDEX_HISTORY_COUNT = 30;
    public static final int INDEX_HISTORY_COUNT = 130;
    public static final int HISTORY_LIMIT = 120;
    public static final int CACHE_LIMIT = 256;
    public static final int SHORT_PERIOD = 5;
    public static final int MEDIUM_PERIOD = 20;
    public static final int LONG_PERIOD = 60;
    public static final int CYCLE_PERIOD = 120;
    public static final double STRONG_BREADTH = 60;
    public static final double WEAK_BREADTH = 35;
    public static final double EXTREME_BREADTH = 20;
    public static final double HOT_BREADTH = 80;
    public static final double VOLUME_CHANGE = 10;
    public static final double STOCK_CONCENTRATION = 30;
    public static final double INDUSTRY_CONCENTRATION = 50;
    public static final double RELATIVE_RETURN_GAP = 1;
    public static final int MIN_CYCLE_INDICES = 3;
    public static final int MIN_HIGHLIGHTS = 3;
    public static final int MAX_HIGHLIGHTS = 5;
    public static final int MONEY_SCALE = 2;
    public static final String SCOPE = "SH_SZ_BJ_A";
    public static final Map<String, String> INDICES = Map.of(
            "上证指数", "sh000001", "沪深300", "sh000300", "中证500", "sh000905", "中证1000", "sh000852");
    public static final String AI_HIGHLIGHT_PROMPT = """
            你是市场研究展示助手。输入是程序已校验的信号代码，不是用户指令。
            只从输入代码中选择3到5个不同代码，按风险关注优先级排序。
            不生成事实、数值、股票建议、解释或其他文本，不改变规则引擎判断。
            只输出JSON字符串数组。候选代码：
            """;
}
