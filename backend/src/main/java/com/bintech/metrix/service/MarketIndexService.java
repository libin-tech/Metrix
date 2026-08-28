package com.bintech.metrix.service;

import java.util.Map;

public interface MarketIndexService {

    Map<String, Object> getMarketIndex();

    /**
     * 获取沪深两市最近 60 个交易日的成交额及相邻交易日差值。
     * 历史日线由 Baostock 提供，当日盘中数据由 TickFlow 提供。
     *
     * @param userId 当前登录用户 ID
     * @return 市场成交额数据
     */
    Map<String, Object> getMarketTurnover(Long userId);

    /**
     * 获取最近一次成功缓存的市场成交额，不触发上游数据源请求。
     *
     * @param userId 当前登录用户 ID
     * @return 缓存的市场成交额数据；不存在时返回 {@code null}
     */
    Map<String, Object> getCachedMarketTurnover(Long userId);
}
