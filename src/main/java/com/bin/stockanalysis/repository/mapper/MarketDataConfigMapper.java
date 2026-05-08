package com.bin.stockanalysis.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bin.stockanalysis.repository.entity.MarketDataConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 市场数据配置Mapper接口
 */
@Mapper
public interface MarketDataConfigMapper extends BaseMapper<MarketDataConfig> {

}