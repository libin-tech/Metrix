package com.bintech.metrix.repository.dao;

import com.bintech.metrix.enums.StockAnalysisStatus;
import com.bintech.metrix.repository.entity.StockAnalysisRecord;

import java.util.List;

public interface StockAnalysisRecordDao {
    int insert(StockAnalysisRecord entity);
    int updateById(StockAnalysisRecord entity);
    int deleteById(Long id);
    int deleteByIdNotIn(List<Long> keepIds);
    StockAnalysisRecord selectById(Long id);
    List<StockAnalysisRecord> selectByUserIdOrderByIdDesc(Long userId);
    List<StockAnalysisRecord> cursorQueryByUserId(Long userId, Long cursor, int limit);
    List<StockAnalysisRecord> selectAllOrderByCreateTimeDesc();
    long countByStockCodeAndStatus(String stockCode, StockAnalysisStatus status);
}
