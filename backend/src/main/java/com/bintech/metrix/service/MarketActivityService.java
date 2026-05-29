package com.bintech.metrix.service;

import java.util.Map;

/**
 * 赚钱效应分析服务
 */
public interface MarketActivityService {

    /**
     * 获取赚钱效应分析数据
     */
    Map<String, Object> getMarketActivity();
}
