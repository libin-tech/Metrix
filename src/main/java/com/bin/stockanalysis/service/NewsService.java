package com.bin.stockanalysis.service;

import com.bin.stockanalysis.dto.request.NewsSourceConfigRequest;
import com.bin.stockanalysis.repository.entity.NewsSourceConfig;

import java.util.List;
import java.util.Map;

public interface NewsService {
    NewsSourceConfig createConfig(NewsSourceConfigRequest request);
    NewsSourceConfig updateConfig(Long id, NewsSourceConfigRequest request);
    NewsSourceConfig getConfigById(Long id);
    List<NewsSourceConfig> getAllConfigs();
    List<NewsSourceConfig> getActiveConfigs();
    void deleteConfig(Long id);
    Map<String, Object> fetchStockNews(String stockCode);
    String summarizeNews(List<Map<String, Object>> newsList);
}
