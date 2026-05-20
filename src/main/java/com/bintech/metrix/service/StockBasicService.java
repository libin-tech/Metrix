package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.PageResult;
import com.bintech.metrix.repository.entity.StockBasic;
import org.springframework.web.multipart.MultipartFile;

/**
 * 股票基础信息服务接口
 *
 * <p>提供股票基础数据的查询、导入等功能，数据来源于Tushare等数据源的基础信息表。
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

    /**
     * 从CSV文件导入股票基础信息
     *
     * @param file CSV文件（格式需符合Tushare stock_basic导出规范）
     * @return 导入结果描述（成功条数/失败原因）
     */
    String importCsv(MultipartFile file);

    /**
     * 根据股票代码获取股票基础信息
     *
     * @param stockCode TS股票代码（如 000001.SZ）
     * @return 股票基础信息，不存在时返回null
     */
    StockBasic getByTsCode(String stockCode);
}
