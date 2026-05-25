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

    /**
     * 创建新闻源配置
     *
     * @param request 配置请求（数据源类型、API Token等）
     * @return 创建的配置
     */
    NewsSourceConfig createConfig(NewsSourceConfigRequest request);

    /**
     * 更新新闻源配置
     *
     * @param id      配置ID
     * @param request 配置请求
     * @return 更新后的配置
     */
    NewsSourceConfig updateConfig(Long id, NewsSourceConfigRequest request);

    /** 根据ID获取配置 */
    NewsSourceConfig getConfigById(Long id);

    /** 获取所有配置 */
    List<NewsSourceConfig> getAllConfigs();

    /** 获取已激活的配置列表 */
    List<NewsSourceConfig> getActiveConfigs();

    /** 删除配置 */
    void deleteConfig(Long id);

    /**
     * 获取指定股票的新闻
     *
     * @param stockBasic 股票基础信息（用于提取股票名称/代码作为搜索关键词）
     * @return 新闻列表及统计信息的映射
     */
    Map<String, Object> fetchStockNews(StockBasic stockBasic);

    /**
     * 使用AI对新闻列表进行摘要总结
     *
     * @param newsList  新闻列表（含标题、摘要、链接等）
     * @param modelType AI模型类型
     * @return 总结后的文本
     */
    String summarizeNews(List<Map<String, Object>> newsList, String modelType);
}
