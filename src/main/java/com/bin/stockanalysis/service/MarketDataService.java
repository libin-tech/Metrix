package com.bin.stockanalysis.service;

import com.bin.stockanalysis.dto.request.MarketDataConfigRequest;
import com.bin.stockanalysis.repository.entity.MarketDataConfig;
import com.bin.stockanalysis.repository.entity.StockBasic;

import java.util.List;
import java.util.Map;

public interface MarketDataService {
    MarketDataConfig createConfig(MarketDataConfigRequest request);
    MarketDataConfig updateConfig(Long id, MarketDataConfigRequest request);
    MarketDataConfig getConfigById(Long id);
    List<MarketDataConfig> getAllConfigs();
    List<MarketDataConfig> getActiveConfigs();
    void deleteConfig(Long id);
    Map<String, Object> fetchRealTimeData(StockBasic stockBasic);
    Map<String, Object> fetchChipData(StockBasic stockBasic);
    Map<String, Object> fetchDepthData(StockBasic stockBasic);
    Map<String, Object> fetchKlinesData(StockBasic stockBasic, int limit);
}
