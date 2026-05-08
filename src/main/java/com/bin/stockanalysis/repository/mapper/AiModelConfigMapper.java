package com.bin.stockanalysis.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bin.stockanalysis.repository.entity.AiModelConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI模型配置Mapper接口
 */
@Mapper
public interface AiModelConfigMapper extends BaseMapper<AiModelConfig> {

}
