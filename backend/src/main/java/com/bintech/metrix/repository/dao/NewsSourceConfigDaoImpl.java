package com.bintech.metrix.repository.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bintech.metrix.repository.entity.NewsSourceConfig;
import com.bintech.metrix.repository.mapper.NewsSourceConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class NewsSourceConfigDaoImpl implements NewsSourceConfigDao {

    private final NewsSourceConfigMapper baseMapper;

    @Override
    public int insert(NewsSourceConfig entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(NewsSourceConfig entity) {
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
        return baseMapper.update(null, new LambdaUpdateWrapper<NewsSourceConfig>()
                .set(NewsSourceConfig::getIsActive, false)
                .eq(NewsSourceConfig::getUserId, userId));
    }

    @Override
    public int deactivateByUserIdAndExcludeId(Long userId, Long excludeId) {
        if (userId == null || excludeId == null) {
            log.warn("deactivateByUserIdAndExcludeId: userId or excludeId is null");
            return 0;
        }
        return baseMapper.update(null, new LambdaUpdateWrapper<NewsSourceConfig>()
                .set(NewsSourceConfig::getIsActive, false)
                .ne(NewsSourceConfig::getId, excludeId)
                .eq(NewsSourceConfig::getUserId, userId));
    }

    @Override
    public NewsSourceConfig selectByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null) {
            log.warn("selectByIdAndUserId: id or userId is null");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<NewsSourceConfig>()
                .eq(NewsSourceConfig::getId, id)
                .eq(NewsSourceConfig::getUserId, userId));
    }

    @Override
    public List<NewsSourceConfig> selectByUserId(Long userId) {
        if (userId == null) {
            log.warn("selectByUserId: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<NewsSourceConfig>()
                .eq(NewsSourceConfig::getUserId, userId));
    }

    @Override
    public List<NewsSourceConfig> selectActiveByUserId(Long userId) {
        if (userId == null) {
            log.warn("selectActiveByUserId: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<NewsSourceConfig>()
                .eq(NewsSourceConfig::getIsActive, true)
                .eq(NewsSourceConfig::getUserId, userId));
    }

    @Override
    public long countByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null) {
            log.warn("countByIdAndUserId: id or userId is null");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<NewsSourceConfig>()
                .eq(NewsSourceConfig::getId, id)
                .eq(NewsSourceConfig::getUserId, userId));
    }

    @Override
    public long countActiveByUserId(Long userId) {
        if (userId == null) {
            log.warn("countActiveByUserId: userId is null");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<NewsSourceConfig>()
                .eq(NewsSourceConfig::getIsActive, true)
                .eq(NewsSourceConfig::getUserId, userId));
    }
}
