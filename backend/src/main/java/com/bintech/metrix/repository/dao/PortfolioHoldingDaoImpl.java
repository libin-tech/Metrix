package com.bintech.metrix.repository.dao;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.repository.entity.PortfolioHolding;
import com.bintech.metrix.repository.mapper.PortfolioHoldingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class PortfolioHoldingDaoImpl implements PortfolioHoldingDao {

    private final PortfolioHoldingMapper baseMapper;

    @Override
    public int insert(PortfolioHolding entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(PortfolioHolding entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public int deleteByAccountId(Long accountId) {
        if (accountId == null) {
            log.warn("deleteByAccountId: accountId is null");
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<PortfolioHolding>()
                .eq(PortfolioHolding::getAccountId, accountId));
    }

    @Override
    public PortfolioHolding selectById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<PortfolioHolding> selectByUserId(Long userId) {
        if (userId == null) {
            log.warn("selectByUserId: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<PortfolioHolding>()
                .eq(PortfolioHolding::getUserId, userId));
    }

    @Override
    public List<PortfolioHolding> selectByUserIdWithCostAndQuantityLimit(Long userId, int limit) {
        if (userId == null) {
            log.warn("selectByUserIdWithCostAndQuantityLimit: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<PortfolioHolding>()
                .eq(PortfolioHolding::getUserId, userId)
                .isNotNull(PortfolioHolding::getCost)
                .isNotNull(PortfolioHolding::getQuantity)
                .last("LIMIT " + limit));
    }

    @Override
    public List<PortfolioHolding> selectByAccountId(Long accountId) {
        if (accountId == null) {
            log.warn("selectByAccountId: accountId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<PortfolioHolding>()
                .eq(PortfolioHolding::getAccountId, accountId));
    }

    @Override
    public long countByUserId(Long userId) {
        if (userId == null) {
            log.warn("countByUserId: userId is null");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<PortfolioHolding>()
                .eq(PortfolioHolding::getUserId, userId));
    }

    @Override
    public long countByUserIdAndAccountIdAndStockCode(Long userId, Long accountId, String stockCode) {
        if (userId == null || accountId == null || StrUtil.isBlank(stockCode)) {
            log.warn("countByUserIdAndAccountIdAndStockCode: params invalid");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<PortfolioHolding>()
                .eq(PortfolioHolding::getUserId, userId)
                .eq(PortfolioHolding::getAccountId, accountId)
                .eq(PortfolioHolding::getStockCode, stockCode));
    }

    @Override
    public long countByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null) {
            log.warn("countByIdAndUserId: id or userId is null");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<PortfolioHolding>()
                .eq(PortfolioHolding::getId, id)
                .eq(PortfolioHolding::getUserId, userId));
    }
}
