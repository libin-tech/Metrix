package com.bintech.metrix.constants;

/**
 * 系统配置常量
 */
public final class SystemConstants {

    private SystemConstants() {}

    /* ==================== 超时时间（秒） ==================== */
    public static final int DEFAULT_TIMEOUT_SECONDS = 60;
    public static final int AI_MODEL_TIMEOUT_SECONDS = 120;
    public static final int CHROME_TIMEOUT_SECONDS = 60;
    public static final int POLL_TIMEOUT_SECONDS = 3;
    public static final int CORS_MAX_AGE_SECONDS = 3600;

    /* ==================== 毫秒转换 ==================== */
    public static final int MILLIS_PER_SECOND = 1000;
    public static final int READER_JOIN_TIMEOUT_MILLIS = 5000;

    /* ==================== 验证限制 ==================== */
    public static final int MAX_MODEL_TYPE_LENGTH = 50;
    public static final int MAX_MODEL_NAME_LENGTH = 100;
    public static final int MAX_BROKER_NAME_LENGTH = 10;
    public static final int MAX_ACCOUNT_NUMBER_LENGTH = 30;
    public static final int MAX_REMARK_LENGTH = 50;
    public static final int MAX_REMARK_LENGTH_100 = 100;
    public static final int PROMPT_PREVIEW_MAX_LENGTH = 50;

    /* ==================== AI模型默认值 ==================== */
    public static final double DEFAULT_TEMPERATURE = 0.7;
    public static final int DEFAULT_MAX_TOKENS = 2048;
    public static final int DEFAULT_REQUEST_INTERVAL = 30;

    /* ==================== K线相关 ==================== */
    public static final String KLINE_PERIOD_DAY = "1d";
    public static final int KLINE_DISPLAY_COUNT = 30;

    /* ==================== 盘口深度 ==================== */
    public static final int DEPTH_MAX_LEVELS = 5;

    /* ==================== Markdown渲染 ==================== */
    public static final String MARKDOWN_H1_PREFIX = "#";
    public static final String TRIPLE_NEWLINE = "\n\n\n";
    public static final int HEADING_MIN_LEVEL = 1;
    public static final int HEADING_MAX_LEVEL = 6;

    /* ==================== 斜杠常量 ==================== */
    public static final String URL_TRAILING_SLASH = "/";
}
