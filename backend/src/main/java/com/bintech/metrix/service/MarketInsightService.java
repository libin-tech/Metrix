package com.bintech.metrix.service;

import java.util.Map;

/**
 * 首页市场洞察数据服务。
 */
public interface MarketInsightService {

    /**
     * 获取龙虎榜和个股资金流向数据。
     *
     * @return 首页展示所需的市场洞察数据
     */
    Map<String, Object> getMarketInsights();
}
