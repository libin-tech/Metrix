package com.bintech.metrix.service;

import com.bintech.metrix.dto.request.NewsSourceConfigRequest;
import com.bintech.metrix.repository.entity.NewsSourceConfig;
import com.bintech.metrix.repository.entity.StockBasic;

import java.util.List;
import java.util.Map;

/**
 * 新闻源配置与搜索服务接口
 *
 * <p>提供新闻源的配置管理以及股票新闻的搜索、摘要等功能，集成博查API获取新闻数据。
 */
public interface NewsService {

    NewsSourceConfig createConfig(NewsSourceConfigRequest request);

    NewsSourceConfig updateConfig(Long id, NewsSourceConfigRequest request);

    NewsSourceConfig getConfigById(Long id);

    List<NewsSourceConfig> getAllConfigs();

    List<NewsSourceConfig> getActiveConfigs();

    void deleteConfig(Long id);

    boolean hasActiveNewsSource(Long userId);

    Map<String, Object> fetchStockNews(StockBasic stockBasic);

    Map<String, Object> fetchStockNews(StockBasic stockBasic, Long userId);

    String summarizeNews(List<Map<String, Object>> newsList, String modelType);
}
