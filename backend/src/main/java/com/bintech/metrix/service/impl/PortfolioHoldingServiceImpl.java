package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.dto.request.PortfolioHoldingRequest;
import com.bintech.metrix.dto.response.PortfolioHoldingListResponse;
import com.bintech.metrix.dto.response.PortfolioHoldingVO;
import com.bintech.metrix.dto.response.PortfolioSummary;
import com.bintech.metrix.repository.entity.BrokerAccount;
import com.bintech.metrix.repository.entity.PortfolioHolding;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.repository.mapper.BrokerAccountMapper;
import com.bintech.metrix.repository.mapper.PortfolioHoldingMapper;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.PortfolioHoldingService;
import com.bintech.metrix.service.StockBasicService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 持仓管理服务实现
 *
 * <p>提供持仓标的的增删查、行情异步刷新（虚拟线程+轮询）、盈亏计算等功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioHoldingServiceImpl implements PortfolioHoldingService {

    private static final int MAX_REFRESH_COUNT = 10;
    private static final int MAX_HOLDING_COUNT = 100;
    private static final int PROFIT_LOSS_SCALE = 4;
    private static final int PRICE_SCALE = 3;

    private final PortfolioHoldingMapper holdingMapper;
    private final BrokerAccountMapper accountMapper;
    private final StockBasicService stockBasicService;
    private final MarketDataService marketDataService;

    private final ConcurrentHashMap<Long, PortfolioHoldingVO> priceRefreshCache = new ConcurrentHashMap<>();
    private final ExecutorService refreshExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @PreDestroy
    public void shutdown() {
        refreshExecutor.close();
    }

    /**
     * 查询持仓列表，支持按券商名称/标的代码/名称模糊搜索
     */
    @Override
    public PortfolioHoldingListResponse getHoldings(String keyword, Long accountId) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<PortfolioHolding> queryWrapper = new LambdaQueryWrapper<PortfolioHolding>()
                .eq(PortfolioHolding::getUserId, userId);
        if (accountId != null) {
            queryWrapper.eq(PortfolioHolding::getAccountId, accountId);
        }
        List<PortfolioHolding> holdings = holdingMapper.selectList(queryWrapper);
        if (holdings.isEmpty()) {
            return new PortfolioHoldingListResponse(List.of(), new PortfolioSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null));
        }

        List<BrokerAccount> accounts = accountMapper.selectList(
                new LambdaQueryWrapper<BrokerAccount>()
                        .eq(BrokerAccount::getUserId, userId));
        Map<Long, BrokerAccount> accountMap = accounts.stream()
                .collect(Collectors.toMap(BrokerAccount::getId, a -> a));

        List<PortfolioHoldingVO> vos = new ArrayList<>();
        LocalDateTime latestRefresh = null;
        for (PortfolioHolding h : holdings) {
            if (h.getCost() == null || h.getQuantity() == null) {
                continue;
            }

            PortfolioHoldingVO vo = buildBaseVO(h, accountMap);
            vos.add(vo);

            if (vo.getCachedPriceTime() != null
                    && (latestRefresh == null || vo.getCachedPriceTime().isAfter(latestRefresh))) {
                latestRefresh = vo.getCachedPriceTime();
            }
        }

        if (StrUtil.isNotBlank(keyword)) {
            String kw = keyword.trim();
            vos = vos.stream()
                    .filter(v -> (v.getBrokerName() != null && v.getBrokerName().contains(kw))
                            || (v.getStockCode() != null && v.getStockCode().contains(kw))
                            || (v.getStockName() != null && v.getStockName().contains(kw)))
                    .collect(Collectors.toList());
        }

        PortfolioSummary summary = calculateSummary(vos, latestRefresh);
        return new PortfolioHoldingListResponse(vos, summary);
    }

    /**
     * 异步刷新持仓实时行情，使用虚拟线程并行获取，结果通过轮询接口获取
     */
    @Override
    public List<PortfolioHoldingVO> refreshPrices() {
        Long userId = StpUtil.getLoginIdAsLong();
        return doRefreshPrices(userId);
    }

    private List<PortfolioHoldingVO> doRefreshPrices(Long userId) {
        List<PortfolioHolding> holdings = holdingMapper.selectList(
                new LambdaQueryWrapper<PortfolioHolding>()
                        .eq(PortfolioHolding::getUserId, userId)
                        .isNotNull(PortfolioHolding::getCost)
                        .isNotNull(PortfolioHolding::getQuantity)
                        .last("LIMIT " + MAX_REFRESH_COUNT));
        if (holdings.isEmpty()) {
            return List.of();
        }

        List<BrokerAccount> accounts = accountMapper.selectList(
                new LambdaQueryWrapper<BrokerAccount>()
                        .eq(BrokerAccount::getUserId, userId));
        Map<Long, BrokerAccount> accountMap = accounts.stream()
                .collect(Collectors.toMap(BrokerAccount::getId, a -> a));

        List<PortfolioHoldingVO> vos = new ArrayList<>();
        for (PortfolioHolding h : holdings) {
            PortfolioHoldingVO vo = buildBaseVO(h, accountMap);
            vos.add(vo);
            Long capturedUserId = userId;
            Long holdingId = h.getId();
            refreshExecutor.submit(() -> {
                try {
                    fetchCurrentPrice(vo, capturedUserId);
                    persistCachedPrice(holdingId, vo.getCurrentPrice());
                    priceRefreshCache.put(vo.getId(), vo);
                } catch (Exception e) {
                    log.warn("异步刷新 {} 行情失败: {}", vo.getStockCode(), e.getMessage());
                }
            });
        }
        return vos;
    }

    /**
     * 轮询获取已刷新完成的实时行情（消费缓存中的刷新结果）
     */
    @Override
    public Map<Long, PortfolioHoldingVO> pollRefreshedPrices(List<Long> ids) {
        Map<Long, PortfolioHoldingVO> result = new HashMap<>();
        for (Long id : ids) {
            PortfolioHoldingVO vo = priceRefreshCache.remove(id);
            if (vo != null) {
                result.put(id, vo);
            }
        }
        return result;
    }

    private PortfolioHoldingVO buildBaseVO(PortfolioHolding h, Map<Long, BrokerAccount> accountMap) {
        PortfolioHoldingVO vo = new PortfolioHoldingVO();
        vo.setId(h.getId());
        vo.setAccountId(h.getAccountId());
        vo.setStockCode(h.getStockCode());
        vo.setStockName(h.getStockName());
        vo.setCost(h.getCost() != null ? h.getCost().setScale(PRICE_SCALE, RoundingMode.HALF_UP) : null);
        vo.setQuantity(h.getQuantity());

        BrokerAccount account = accountMap.get(h.getAccountId());
        if (account != null) {
            vo.setBrokerName(account.getBrokerName());
            vo.setAccountNumber(account.getAccountNumber());
        }

        if (h.getCachedPrice() != null) {
            BigDecimal cachedPrice = h.getCachedPrice().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            vo.setCachedPrice(cachedPrice);
            vo.setCachedPriceTime(h.getCachedPriceTime());
            vo.setCurrentPrice(cachedPrice);
            BigDecimal costTotal = h.getCost().multiply(h.getQuantity());
            BigDecimal currentTotal = cachedPrice.multiply(h.getQuantity());
            BigDecimal plAmount = currentTotal.subtract(costTotal);
            vo.setProfitLossAmount(plAmount);
            calcProfitLossPercent(vo, costTotal, plAmount);
        }

        return vo;
    }

    private void fetchCurrentPrice(PortfolioHoldingVO vo, Long userId) {
        try {
            StockBasic stockBasic = stockBasicService.getByTsCode(vo.getStockCode());
            if (stockBasic == null) {
                return;
            }
            Map<String, Object> marketData = marketDataService.fetchRealTimeData(stockBasic, userId);
            BigDecimal currentPrice = extractCurrentPrice(marketData);
            if (currentPrice == null) {
                return;
            }
            vo.setCurrentPrice(currentPrice.setScale(PRICE_SCALE, RoundingMode.HALF_UP));
            BigDecimal costTotal = vo.getCost().multiply(vo.getQuantity());
            BigDecimal currentTotal = currentPrice.multiply(vo.getQuantity());
            BigDecimal plAmount = currentTotal.subtract(costTotal);
            vo.setProfitLossAmount(plAmount);
            calcProfitLossPercent(vo, costTotal, plAmount);
        } catch (Exception e) {
            log.warn("获取 {} 实时行情失败: {}", vo.getStockCode(), e.getMessage());
        }
    }

    private void calcProfitLossPercent(PortfolioHoldingVO vo, BigDecimal costTotal, BigDecimal plAmount) {
        if (costTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal plPercent = plAmount
                .divide(costTotal, PROFIT_LOSS_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        vo.setProfitLossPercent(plPercent);
    }

    private BigDecimal extractCurrentPrice(Map<String, Object> marketData) {
        if (marketData == null) {
            return null;
        }
        JSONObject marketDataJson = new JSONObject(marketData);
        try {
            if (!"success".equals(marketDataJson.get("status"))) return null;
            JSONArray data = marketDataJson.getJSONArray("data");
            if (data == null || data.isEmpty()) return null;

            JSONObject latestData = data.getJSONObject(0);
            return latestData.getBigDecimal("last_price");
        } catch (Exception e) {
            log.error("解析市场数据失败: {}", e.getMessage(), e);
        }
        return null;
    }

    private void persistCachedPrice(Long holdingId, BigDecimal currentPrice) {
        if (holdingId == null || currentPrice == null) {
            return;
        }
        PortfolioHolding entity = new PortfolioHolding();
        entity.setId(holdingId);
        entity.setCachedPrice(currentPrice);
        entity.setCachedPriceTime(LocalDateTime.now());
        holdingMapper.updateById(entity);
    }

    private PortfolioSummary calculateSummary(List<PortfolioHoldingVO> vos, LocalDateTime latestRefresh) {
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalCostValue = BigDecimal.ZERO;
        for (PortfolioHoldingVO vo : vos) {
            if (vo.getCurrentPrice() != null && vo.getQuantity() != null && vo.getCost() != null) {
                totalMarketValue = totalMarketValue.add(vo.getCurrentPrice().multiply(vo.getQuantity()));
                totalCostValue = totalCostValue.add(vo.getCost().multiply(vo.getQuantity()));
            }
        }
        BigDecimal totalProfitLossAmount = totalMarketValue.subtract(totalCostValue);
        BigDecimal totalProfitLossPercent = BigDecimal.ZERO;
        if (totalCostValue.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitLossPercent = totalProfitLossAmount
                    .divide(totalCostValue, PROFIT_LOSS_SCALE, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return new PortfolioSummary(totalMarketValue, totalProfitLossPercent, totalProfitLossAmount, latestRefresh);
    }

    /**
     * 新增持仓标的，校验数量上限和重复性
     */
    @Override
    @Transactional
    public void createHolding(PortfolioHoldingRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        long count = holdingMapper.selectCount(
                new LambdaQueryWrapper<PortfolioHolding>()
                        .eq(PortfolioHolding::getUserId, userId));
        if (count >= MAX_HOLDING_COUNT) {
            throw new RuntimeException("持仓数量已达上限（" + MAX_HOLDING_COUNT + "个），请先删除部分持仓再添加");
        }

        Long existing = holdingMapper.selectCount(new LambdaQueryWrapper<PortfolioHolding>()
                .eq(PortfolioHolding::getUserId, userId)
                .eq(PortfolioHolding::getAccountId, request.getAccountId())
                .eq(PortfolioHolding::getStockCode, request.getStockCode()));
        if (existing > 0) {
            throw new RuntimeException("该账户下已存在该标的，请勿重复添加");
        }

        PortfolioHolding holding = new PortfolioHolding();
        holding.setAccountId(request.getAccountId());
        holding.setStockCode(request.getStockCode());
        holding.setStockName(request.getStockName());
        holding.setCost(request.getCost().setScale(PRICE_SCALE, RoundingMode.HALF_UP));
        holding.setQuantity(request.getQuantity());
        holding.setUserId(userId);
        holding.setCreateTime(LocalDateTime.now());
        holding.setUpdateTime(LocalDateTime.now());
        holdingMapper.insert(holding);
    }

    /**
     * 批量新增持仓标的，校验重复性和数量上限
     */
    @Override
    @Transactional
    public void batchCreateHoldings(Long accountId, List<PortfolioHoldingRequest> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("批量添加列表不能为空");
        }

        long currentCount = holdingMapper.selectCount(
                new LambdaQueryWrapper<PortfolioHolding>()
                        .eq(PortfolioHolding::getUserId, userId));

        Set<String> batchCodes = new HashSet<>();
        for (PortfolioHoldingRequest item : items) {
            if (!batchCodes.add(item.getStockCode())) {
                throw new RuntimeException("批量列表中存在重复标的：" + item.getStockCode());
            }

            Long existing = holdingMapper.selectCount(new LambdaQueryWrapper<PortfolioHolding>()
                    .eq(PortfolioHolding::getUserId, userId)
                    .eq(PortfolioHolding::getAccountId, accountId)
                    .eq(PortfolioHolding::getStockCode, item.getStockCode()));
            if (existing > 0) {
                throw new RuntimeException("该账户下已存在标的：" + item.getStockCode());
            }
        }

        if (currentCount + items.size() > MAX_HOLDING_COUNT) {
            throw new RuntimeException("持仓数量已达上限（" + MAX_HOLDING_COUNT + "个），请先删除部分持仓再添加");
        }

        LocalDateTime now = LocalDateTime.now();
        for (PortfolioHoldingRequest item : items) {
            PortfolioHolding holding = new PortfolioHolding();
            holding.setAccountId(accountId);
            holding.setStockCode(item.getStockCode());
            holding.setStockName(item.getStockName());
            holding.setCost(item.getCost() != null ? item.getCost().setScale(PRICE_SCALE, RoundingMode.HALF_UP) : null);
            holding.setQuantity(item.getQuantity());
            holding.setUserId(userId);
            holding.setCreateTime(now);
            holding.setUpdateTime(now);
            holdingMapper.insert(holding);
        }
    }

    @Override
    @Transactional
    public void deleteHolding(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long count = holdingMapper.selectCount(
                new LambdaQueryWrapper<PortfolioHolding>()
                        .eq(PortfolioHolding::getId, id)
                        .eq(PortfolioHolding::getUserId, userId));
        if (count == 0) {
            throw new RuntimeException("持仓记录不存在");
        }
        holdingMapper.deleteById(id);
    }

    /**
     * 获取当前用户所有持仓的股票代码集合
     */
    @Override
    public Set<String> getHoldingStockCodes() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<PortfolioHolding> holdings = holdingMapper.selectList(
                new LambdaQueryWrapper<PortfolioHolding>()
                        .eq(PortfolioHolding::getUserId, userId));
        return holdings.stream()
                .map(PortfolioHolding::getStockCode)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
