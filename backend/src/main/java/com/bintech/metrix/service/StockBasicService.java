package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.PageResult;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.dto.response.StockSyncResult;

/**
 * 股票基础信息服务接口
 *
 * <p>提供股票基础数据的查询、增量同步等功能，数据来源于同花顺金融数据 API。
 */
public interface StockBasicService {

    /**
     * 分页查询股票列表
     *
     * @param keyword 搜索关键词（股票代码或名称）
     * @param page    页码（从1开始）
     * @param size    每页条数
     * @return 分页结果
     */
    PageResult<StockBasic> pageQuery(String keyword, int page, int size);

    /** 使用当前用户的行情配置同步 A 股标的。 */
    StockSyncResult sync(Long userId);

    /**
     * 根据股票代码获取股票基础信息
     *
     * @param stockCode TS股票代码（如 000001.SZ）
     * @return 股票基础信息，不存在时返回null
     */
    StockBasic getByTsCode(String stockCode);
}
