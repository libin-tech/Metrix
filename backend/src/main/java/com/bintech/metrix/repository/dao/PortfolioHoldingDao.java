package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.PortfolioHolding;

import java.util.List;

public interface PortfolioHoldingDao {
    int insert(PortfolioHolding entity);
    int updateById(PortfolioHolding entity);
    int deleteById(Long id);
    int deleteByAccountId(Long accountId);
    PortfolioHolding selectById(Long id);
    List<PortfolioHolding> selectByUserId(Long userId);
    List<PortfolioHolding> selectByUserIdWithCostAndQuantityLimit(Long userId, int limit);
    List<PortfolioHolding> selectByAccountId(Long accountId);
    long countByUserId(Long userId);
    long countByUserIdAndAccountIdAndStockCode(Long userId, Long accountId, String stockCode);
    long countByIdAndUserId(Long id, Long userId);
}
