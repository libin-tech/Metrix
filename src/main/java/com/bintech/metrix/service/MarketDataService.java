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

    /**
     * 创建行情源配置
     *
     * @param request 配置请求
     * @return 创建的配置
     */
    MarketDataConfig createConfig(MarketDataConfigRequest request);

    /**
     * 更新行情源配置
     *
     * @param id      配置ID
     * @param request 配置请求
     * @return 更新后的配置
     */
    MarketDataConfig updateConfig(Long id, MarketDataConfigRequest request);

    /** 根据ID获取配置 */
    MarketDataConfig getConfigById(Long id);

    /** 获取所有配置 */
    List<MarketDataConfig> getAllConfigs();

    /** 获取已激活的配置列表 */
    List<MarketDataConfig> getActiveConfigs();

    /** 删除配置 */
    void deleteConfig(Long id);

    /**
     * 获取实时行情数据
     *
     * @param stockBasic 股票基础信息
     * @return 行情数据映射
     */
    Map<String, Object> fetchRealTimeData(StockBasic stockBasic);

    /**
     * 获取筹码分布数据
     *
     * @param stockBasic 股票基础信息
     * @return 筹码数据映射
     */
    Map<String, Object> fetchChipData(StockBasic stockBasic);

    /**
     * 获取五档深度数据
     *
     * @param stockBasic 股票基础信息
     * @return 深度数据映射
     */
    Map<String, Object> fetchDepthData(StockBasic stockBasic);

    /**
     * 获取K线数据
     *
     * @param stockBasic 股票基础信息
     * @param limit      获取的K线根数
     * @return K线数据映射
     */
    Map<String, Object> fetchKlinesData(StockBasic stockBasic, int limit);
}
