package com.bin.stockanalysis.service;

import com.bin.stockanalysis.dto.request.MarketDataConfigRequest;
import com.bin.stockanalysis.repository.entity.MarketDataConfig;

import java.util.List;
import java.util.Map;

public interface MarketDataService {
    MarketDataConfig createConfig(MarketDataConfigRequest request);
    MarketDataConfig updateConfig(Long id, MarketDataConfigRequest request);
    MarketDataConfig getConfigById(Long id);
    List<MarketDataConfig> getAllConfigs();
    List<MarketDataConfig> getActiveConfigs();
    void deleteConfig(Long id);
    Map<String, Object> fetchMarketData(String stockCode);
    Map<String, Object> fetchRealTimeData(String stockCode);
}
