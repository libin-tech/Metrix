package com.bin.stockanalysis.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bin.stockanalysis.repository.entity.StockAnalysisRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 股票分析记录Mapper接口
 */
@Mapper
public interface StockAnalysisRecordMapper extends BaseMapper<StockAnalysisRecord> {

}
