package com.bintech.metrix.service;

import com.bintech.metrix.dto.request.MarketDataConfigRequest;
import com.bintech.metrix.repository.entity.MarketDataConfig;
import com.bintech.metrix.repository.entity.StockBasic;

import java.util.List;
import java.util.Map;

/**
 * 市场数据服务接口
 *
 * <p>提供配置管理和行情数据获取功能，包括实时行情、深度数据、K线、筹码分布等。
 */
public interface MarketDataService {

    MarketDataConfig createConfig(MarketDataConfigRequest request);

    MarketDataConfig updateConfig(Long id, MarketDataConfigRequest request);

    MarketDataConfig getConfigById(Long id);

    List<MarketDataConfig> getAllConfigs();

    List<MarketDataConfig> getActiveConfigs();

    void deleteConfig(Long id);

    boolean hasActiveConfig(Long userId);

    Map<String, Object> fetchRealTimeData(StockBasic stockBasic);

    Map<String, Object> fetchRealTimeData(StockBasic stockBasic, Long userId);

    Map<String, Object> fetchChipData(StockBasic stockBasic);

    Map<String, Object> fetchChipData(StockBasic stockBasic, Long userId);

    Map<String, Object> fetchDepthData(StockBasic stockBasic);

    Map<String, Object> fetchDepthData(StockBasic stockBasic, Long userId);

    Map<String, Object> fetchKlinesData(StockBasic stockBasic, int limit);

    Map<String, Object> fetchKlinesData(StockBasic stockBasic, int limit, Long userId);

    Map<String, Object> fetchTopFreeShareholdersData(StockBasic stockBasic);

    Map<String, Object> fetchTopFreeShareholdersData(StockBasic stockBasic, Long userId);
}
