package com.bintech.metrix.constants;

import java.math.BigDecimal;

/**
 * 业务逻辑常量
 */
public final class BusinessConstants {

    private BusinessConstants() {}

    /* ==================== 均线/支撑/压力周期 ==================== */
    public static final int MA_PERIOD_5 = 5;
    public static final int MA_PERIOD_20 = 20;
    public static final int MA_PERIOD_60 = 60;
    public static final int SUPPORT_LOOKBACK = 20;



    /* ==================== 筹码分布阈值 ==================== */
    public static final BigDecimal CHIP_BALANCE_PERCENT = BigDecimal.valueOf(50);
    public static final BigDecimal CHIP_CONCENTRATION_FACTOR = BigDecimal.valueOf(0.8);
    public static final BigDecimal CHIP_BASE_SCORE = BigDecimal.valueOf(50);
    public static final BigDecimal PROFIT_HIGH_THRESHOLD = BigDecimal.valueOf(70);
    public static final BigDecimal PROFIT_MID_THRESHOLD = BigDecimal.valueOf(40);
    public static final BigDecimal DEFAULT_CHIP_CONCENTRATION = BigDecimal.valueOf(55.5);
    public static final BigDecimal DEFAULT_PROFIT_RATIO = BigDecimal.valueOf(50);
    public static final BigDecimal DEFAULT_LOSS_RATIO = BigDecimal.valueOf(50);

    /* ==================== 价格/百分比率 ==================== */
    public static final BigDecimal PCT_MULTIPLIER = BigDecimal.valueOf(100);
    public static final BigDecimal CHANGE_PCT_THRESHOLD = BigDecimal.valueOf(1);
    public static final BigDecimal TURNOVER_RATE_THRESHOLD = BigDecimal.valueOf(1);

    /* ==================== 入场/止损/目标价因子 ==================== */
    public static final BigDecimal IDEAL_ENTRY_FACTOR = BigDecimal.valueOf(0.99);
    public static final BigDecimal SUBOPTIMAL_ENTRY_FACTOR = BigDecimal.valueOf(0.98);
    public static final BigDecimal STOP_LOSS_FACTOR = BigDecimal.valueOf(0.97);
    public static final BigDecimal TARGET_PRICE_FACTOR = BigDecimal.valueOf(1.10);
    public static final BigDecimal DEFAULT_RISK_REWARD_RATIO = BigDecimal.valueOf(1.2);

    /* ==================== 入场/止损描述 ==================== */
    public static final String IDEAL_ENTRY_DESC = "回踩MA5支撑且乖离率修复至安全区";
    public static final String SUBOPTIMAL_ENTRY_DESC = "回踩MA20强支撑，技术修复更充分";
    public static final String STOP_LOSS_DESC = "跌破MA20下方3%，技术形态破位";
    public static final String TARGET_DESC = "前高压力位，风险回报比约1:1.2";

    /* ==================== 新闻相关 ==================== */
    public static final String SOURCE_NAME_BOCHA = "BOCHA";
    public static final String BOCHA_API_PATH = "/v1/web-search";
    public static final int DEFAULT_NEWS_COUNT = 10;
    public static final String NEWS_FRESHNESS = "oneWeek";
    public static final String BOCHA_SEARCH_QUERY = "搜索 %s %s 股票的当前时间近一周内最相关的重要新闻、公告、舆情信息";
    public static final String SUMMARIZE_PROMPT = "请对以下股票相关新闻进行总结分析：\n\n%s\n\n请提供简洁的总结，包括主要事件、市场影响和投资建议。";
    public static final String KEY_WEB_PAGES = "webPages";
    public static final String KEY_TOTAL_ESTIMATED_MATCHES = "totalEstimatedMatches";
    public static final String KEY_SUMMARY = "summary";
    public static final String KEY_SNIPPET = "snippet";
    public static final String KEY_SITE_NAME = "siteName";
    public static final String KEY_DATE_PUBLISHED = "datePublished";

    /* ==================== 分析记录相关 ==================== */
    public static final int MAX_RECORD_KEEP_COUNT = 50;
    public static final int SCHEDULED_CLEANUP_KEEP_COUNT = 200;
    public static final int DEFAULT_KLINE_LIMIT = 60;

    /* ==================== 股票搜索 ==================== */
    public static final String DEFAULT_ANALYSIS_TYPE = "COMPREHENSIVE";

    /* ==================== 模型类型 ==================== */
    public static final String MODEL_TYPE_OPENAI = "OPENAI";
    public static final String MODEL_TYPE_OLLAMA = "OLLAMA";
    public static final String MODEL_TYPE_GEMINI = "GEMINI";
    public static final String TEST_CONNECTION_PROMPT = "Say just 'ok'";

    /* ==================== 通知渠道 ==================== */
    public static final String CHANNEL_TYPE_FEISHU = "FEISHU";
    public static final String FEISHU_SUCCESS_STATUS = "ok";

    /* ==================== 时间格式 ==================== */
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /* ==================== 问一问对话限制 ==================== */
    public static final int MAX_CHAT_SESSIONS_PER_USER = 100;
    public static final int MAX_MESSAGES_PER_SESSION = 20;

    /* ==================== 默认用户 ==================== */
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    public static final String DEFAULT_ADMIN_EMAIL = "admin@example.com";

    /* ==================== SAAS ==================== */
    public static final int LOGIN_CODE_LENGTH = 6;
    public static final String WECHAT_LOGIN_TRIGGER_KEYWORD = "验证码";
}
