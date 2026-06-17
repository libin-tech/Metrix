package com.bintech.metrix.repository.dao;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.repository.entity.MarketReview;
import com.bintech.metrix.repository.mapper.MarketReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class MarketReviewDaoImpl implements MarketReviewDao {

    private final MarketReviewMapper baseMapper;

    @Override
    public int insert(MarketReview entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(MarketReview entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public int deleteByUserIdAndIdNotIn(Long userId, List<Long> keepIds) {
        if (userId == null || CollUtil.isEmpty(keepIds)) {
            log.warn("deleteByUserIdAndIdNotIn: userId is null or keepIds is empty");
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<MarketReview>()
                .eq(MarketReview::getUserId, userId)
                .notIn(MarketReview::getId, keepIds));
    }

    @Override
    public MarketReview selectById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public MarketReview selectByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null) {
            log.warn("selectByIdAndUserId: id or userId is null");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<MarketReview>()
                .eq(MarketReview::getId, id)
                .eq(MarketReview::getUserId, userId));
    }

    @Override
    public MarketReview selectByReviewDateAndUserId(String reviewDate, Long userId) {
        if (StrUtil.isBlank(reviewDate) || userId == null) {
            log.warn("selectByReviewDateAndUserId: reviewDate is blank or userId is null");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<MarketReview>()
                .eq(MarketReview::getReviewDate, reviewDate)
                .eq(MarketReview::getUserId, userId));
    }

    @Override
    public List<MarketReview> selectByUserIdOrderByReviewDateDesc(Long userId) {
        if (userId == null) {
            log.warn("selectByUserIdOrderByReviewDateDesc: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<MarketReview>()
                .eq(MarketReview::getUserId, userId)
                .orderByDesc(MarketReview::getReviewDate));
    }

    @Override
    public List<MarketReview> cursorQueryByUserId(Long userId, Long cursor, int limit) {
        if (userId == null) {
            log.warn("cursorQueryByUserId: userId is null");
            return List.of();
        }
        LambdaQueryWrapper<MarketReview> wrapper = new LambdaQueryWrapper<MarketReview>()
                .eq(MarketReview::getUserId, userId);
        if (cursor != null && cursor > 0) {
            wrapper.lt(MarketReview::getId, cursor);
        }
        wrapper.orderByDesc(MarketReview::getId);
        wrapper.last("LIMIT " + (limit + 1));
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<MarketReview> selectTopByUserIdOrderByReviewDateDesc(Long userId, int limit) {
        if (userId == null) {
            log.warn("selectTopByUserIdOrderByReviewDateDesc: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<MarketReview>()
                .eq(MarketReview::getUserId, userId)
                .orderByDesc(MarketReview::getReviewDate)
                .last("LIMIT " + limit));
    }

    @Override
    public long countByUserId(Long userId) {
        if (userId == null) {
            log.warn("countByUserId: userId is null");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<MarketReview>()
                .eq(MarketReview::getUserId, userId));
    }
}
