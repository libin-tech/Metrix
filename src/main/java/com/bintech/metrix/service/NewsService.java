package com.bintech.metrix.service;

import com.bintech.metrix.dto.request.NewsSourceConfigRequest;
import com.bintech.metrix.repository.entity.NewsSourceConfig;
import com.bintech.metrix.repository.entity.StockBasic;

import java.util.List;
import java.util.Map;

public interface NewsService {
    NewsSourceConfig createConfig(NewsSourceConfigRequest request);
    NewsSourceConfig updateConfig(Long id, NewsSourceConfigRequest request);
    NewsSourceConfig getConfigById(Long id);
    List<NewsSourceConfig> getAllConfigs();
    List<NewsSourceConfig> getActiveConfigs();
    void deleteConfig(Long id);
    Map<String, Object> fetchStockNews(StockBasic stockBasic);
    String summarizeNews(List<Map<String, Object>> newsList, String modelType);
}
