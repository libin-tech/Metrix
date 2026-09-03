package com.bintech.metrix.constants;

public final class CacheConstants {

    private CacheConstants() {}

    public static final String MARKET_TURNOVER_LAST_SUCCESS_KEY_PREFIX = "metrix:market:turnover:last-success:";
    public static final String MARKET_TURNOVER_REFRESH_AT_KEY_PREFIX = "metrix:market:turnover:refresh-at:";
    public static final String MARKET_TURNOVER_LEGACY_LAST_SUCCESS_KEY = "metrix:market:turnover:last-success";
    public static final String MARKET_DASHBOARD_ACTIVITY_LAST_SUCCESS_KEY = "metrix:market:dashboard:activity:last-success";
    public static final String MARKET_DASHBOARD_INDEX_LAST_SUCCESS_KEY = "metrix:market:dashboard:index:last-success";
    public static final String MARKET_DASHBOARD_INSIGHTS_LAST_SUCCESS_KEY = "metrix:market:dashboard:insights:last-success";
    public static final String PORTFOLIO_PRICE_REFRESH_KEY_PREFIX = "metrix:portfolio:price-refresh:";
    public static final String AUTH_CAPTCHA_KEY_PREFIX = "metrix:auth:captcha:";
    public static final String AUTH_EMAIL_CODE_KEY_PREFIX = "metrix:auth:email-code:";
    public static final String AUTH_EMAIL_SEND_COOLDOWN_KEY_PREFIX = "metrix:auth:email-cooldown:";

    public static final long PORTFOLIO_PRICE_REFRESH_TTL_SECONDS = 300L;
    public static final long AUTH_CAPTCHA_TTL_SECONDS = 300L;
    public static final long AUTH_EMAIL_CODE_TTL_SECONDS = 300L;
    public static final long AUTH_EMAIL_SEND_COOLDOWN_SECONDS = 60L;
}
