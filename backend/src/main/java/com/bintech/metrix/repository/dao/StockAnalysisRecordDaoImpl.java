package com.bintech.metrix.repository.dao;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.enums.StockAnalysisStatus;
import com.bintech.metrix.repository.entity.StockAnalysisRecord;
import com.bintech.metrix.repository.mapper.StockAnalysisRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class StockAnalysisRecordDaoImpl implements StockAnalysisRecordDao {

    private final StockAnalysisRecordMapper baseMapper;

    @Override
    public int insert(StockAnalysisRecord entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(StockAnalysisRecord entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public int deleteByIdNotIn(List<Long> keepIds) {
        if (CollUtil.isEmpty(keepIds)) {
            log.warn("deleteByIdNotIn: keepIds is empty, would delete all records");
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<StockAnalysisRecord>()
                .notIn(StockAnalysisRecord::getId, keepIds));
    }

    @Override
    public StockAnalysisRecord selectById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<StockAnalysisRecord> selectByUserIdOrderByIdDesc(Long userId) {
        if (userId == null) {
            log.warn("selectByUserIdOrderByIdDesc: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<StockAnalysisRecord>()
                .eq(StockAnalysisRecord::getUserId, userId)
                .orderByDesc(StockAnalysisRecord::getId));
    }

    @Override
    public List<StockAnalysisRecord> cursorQueryByUserId(Long userId, Long cursor, int limit) {
        if (userId == null) {
            log.warn("cursorQueryByUserId: userId is null");
            return List.of();
        }
        LambdaQueryWrapper<StockAnalysisRecord> wrapper = new LambdaQueryWrapper<StockAnalysisRecord>()
                .eq(StockAnalysisRecord::getUserId, userId);
        if (cursor != null && cursor > 0) {
            wrapper.lt(StockAnalysisRecord::getId, cursor);
        }
        wrapper.orderByDesc(StockAnalysisRecord::getId);
        wrapper.last("LIMIT " + (limit + 1));
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<StockAnalysisRecord> selectAllOrderByCreateTimeDesc() {
        return baseMapper.selectList(new LambdaQueryWrapper<StockAnalysisRecord>()
                .orderByDesc(StockAnalysisRecord::getCreateTime));
    }

    @Override
    public long countByStockCodeAndStatus(String stockCode, StockAnalysisStatus status) {
        if (StrUtil.isBlank(stockCode) || status == null) {
            log.warn("countByStockCodeAndStatus: stockCode or status is null");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<StockAnalysisRecord>()
                .eq(StockAnalysisRecord::getStockCode, stockCode)
                .eq(StockAnalysisRecord::getStatus, status));
    }
}
