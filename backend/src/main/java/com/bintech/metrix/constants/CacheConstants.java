package com.bintech.metrix.constants;

public final class CacheConstants {

    private CacheConstants() {}

    public static final String MARKET_TURNOVER_LAST_SUCCESS_KEY_PREFIX = "metrix:market:turnover:last-success:";
    public static final String MARKET_TURNOVER_REFRESH_AT_KEY_PREFIX = "metrix:market:turnover:refresh-at:";
    public static final String MARKET_TURNOVER_LEGACY_LAST_SUCCESS_KEY = "metrix:market:turnover:last-success";
    public static final String WECHAT_ACCESS_TOKEN_KEY = "metrix:wechat:access-token";
    public static final String WECHAT_LOGIN_CODE_KEY_PREFIX = "metrix:wechat:login-code:";
    public static final String PORTFOLIO_PRICE_REFRESH_KEY_PREFIX = "metrix:portfolio:price-refresh:";

    public static final long WECHAT_ACCESS_TOKEN_TTL_SECONDS = 7000L;
    public static final long WECHAT_LOGIN_CODE_TTL_SECONDS = 300L;
    public static final long PORTFOLIO_PRICE_REFRESH_TTL_SECONDS = 300L;
}
