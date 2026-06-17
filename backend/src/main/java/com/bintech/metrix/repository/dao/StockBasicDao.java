package com.bintech.metrix.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.repository.entity.StockBasic;

import java.util.List;

public interface StockBasicDao {
    int insert(StockBasic entity);
    int updateById(StockBasic entity);
    int deleteById(Long id);
    StockBasic selectById(Long id);
    StockBasic selectByTsCode(String tsCode);
    StockBasic selectBySymbol(String symbol);
    StockBasic selectByName(String name);
    StockBasic selectByNameLike(String name);
    List<StockBasic> selectByTsCodeIn(List<String> tsCodes);
    List<StockBasic> selectLikeNameOrTsCode(String keyword);
    IPage<StockBasic> selectStockPage(Page<StockBasic> page, String keyword);
}
