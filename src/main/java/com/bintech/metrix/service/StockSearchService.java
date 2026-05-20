package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.StockInfo;

import java.util.List;

/**
 * 股票搜索服务接口
 *
 * <p>提供关键字搜索股票的功能，返回前端自动补全所需的精简股票信息。
 */
public interface StockSearchService {

    /**
     * 搜索股票
     *
     * @param keyword 搜索关键词（代码或名称的模糊匹配）
     * @return 匹配的股票信息列表
     */
    List<StockInfo> searchStocks(String keyword);
}
