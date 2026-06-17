package com.bintech.metrix.repository.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bintech.metrix.repository.entity.MarketDataConfig;
import com.bintech.metrix.repository.mapper.MarketDataConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class MarketDataConfigDaoImpl implements MarketDataConfigDao {

    private final MarketDataConfigMapper baseMapper;

    @Override
    public int insert(MarketDataConfig entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(MarketDataConfig entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public int deactivateByUserId(Long userId) {
        if (userId == null) {
            log.warn("deactivateByUserId: userId is null");
            return 0;
        }
        return baseMapper.update(null, new LambdaUpdateWrapper<MarketDataConfig>()
                .set(MarketDataConfig::getIsActive, false)
                .eq(MarketDataConfig::getUserId, userId));
    }

    @Override
    public int deactivateByUserIdAndExcludeId(Long userId, Long excludeId) {
        if (userId == null || excludeId == null) {
            log.warn("deactivateByUserIdAndExcludeId: userId or excludeId is null");
            return 0;
        }
        return baseMapper.update(null, new LambdaUpdateWrapper<MarketDataConfig>()
                .set(MarketDataConfig::getIsActive, false)
                .ne(MarketDataConfig::getId, excludeId)
                .eq(MarketDataConfig::getUserId, userId));
    }

    @Override
    public MarketDataConfig selectByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null) {
            log.warn("selectByIdAndUserId: id or userId is null");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<MarketDataConfig>()
                .eq(MarketDataConfig::getId, id)
                .eq(MarketDataConfig::getUserId, userId));
    }

    @Override
    public List<MarketDataConfig> selectByUserId(Long userId) {
        if (userId == null) {
            log.warn("selectByUserId: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<MarketDataConfig>()
                .eq(MarketDataConfig::getUserId, userId));
    }

    @Override
    public List<MarketDataConfig> selectActiveByUserId(Long userId) {
        if (userId == null) {
            log.warn("selectActiveByUserId: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<MarketDataConfig>()
                .eq(MarketDataConfig::getIsActive, true)
                .eq(MarketDataConfig::getUserId, userId));
    }

    @Override
    public long countByUserId(Long userId) {
        if (userId == null) {
            log.warn("countByUserId: userId is null");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<MarketDataConfig>()
                .eq(MarketDataConfig::getUserId, userId));
    }
}
