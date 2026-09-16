package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.bintech.metrix.constants.CacheConstants;
import com.bintech.metrix.dto.request.PortfolioHoldingRequest;
import com.bintech.metrix.dto.request.TakeProfitPlanRequest;
import com.bintech.metrix.dto.response.PortfolioHoldingListResponse;
import com.bintech.metrix.dto.response.PortfolioHoldingVO;
import com.bintech.metrix.dto.response.PortfolioSummary;
import com.bintech.metrix.dto.response.TakeProfitPlanVO;
import com.bintech.metrix.repository.dao.BrokerAccountDao;
import com.bintech.metrix.repository.dao.PortfolioHoldingDao;
import com.bintech.metrix.repository.entity.BrokerAccount;
import com.bintech.metrix.repository.entity.PortfolioHolding;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.PortfolioHoldingService;
import com.bintech.metrix.service.RedisCacheService;
import com.bintech.metrix.service.StockBasicService;
import com.bintech.metrix.service.TakeProfitPlanStatusResolver;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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

    private final PortfolioHoldingDao holdingDao;
    private final BrokerAccountDao accountDao;
    private final StockBasicService stockBasicService;
    private final MarketDataService marketDataService;
    private final RedisCacheService redisCacheService;

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
        List<PortfolioHolding> holdings = holdingDao.selectByUserId(userId);
        if (accountId != null) {
            holdings = holdings.stream()
                    .filter(h -> accountId.equals(h.getAccountId()))
                    .collect(Collectors.toList());
        }
        if (holdings.isEmpty()) {
            return new PortfolioHoldingListResponse(List.of(), new PortfolioSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null));
        }

        List<BrokerAccount> accounts = accountDao.selectByUserId(userId);
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
        List<PortfolioHolding> holdings = holdingDao.selectByUserIdWithCostAndQuantityLimit(userId, MAX_REFRESH_COUNT);
        if (holdings.isEmpty()) {
            return List.of();
        }

        List<BrokerAccount> accounts = accountDao.selectByUserId(userId);
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
                    persistCachedPrice(holdingId, vo);
                    cachePriceRefreshResult(capturedUserId, vo);
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
        Long userId = StpUtil.getLoginIdAsLong();
        Map<Long, PortfolioHoldingVO> result = new HashMap<>();
        for (Long id : ids) {
            PortfolioHoldingVO vo = redisCacheService.getAndDeleteJson(getPriceRefreshCacheKey(userId, id),
                    PortfolioHoldingVO.class);
            if (vo != null) {
                result.put(id, vo);
            }
        }
        return result;
    }

    private void cachePriceRefreshResult(Long userId, PortfolioHoldingVO vo) {
        redisCacheService.setJson(getPriceRefreshCacheKey(userId, vo.getId()), vo,
                Duration.ofSeconds(CacheConstants.PORTFOLIO_PRICE_REFRESH_TTL_SECONDS));
    }

    private String getPriceRefreshCacheKey(Long userId, Long holdingId) {
        return CacheConstants.PORTFOLIO_PRICE_REFRESH_KEY_PREFIX + userId + ":" + holdingId;
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
        vo.setTakeProfitPlan(buildTakeProfitPlan(h, vo.getCurrentPrice()));

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
            refreshTakeProfitPlanStatus(vo.getTakeProfitPlan(), currentPrice);
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

    private void persistCachedPrice(Long holdingId, PortfolioHoldingVO vo) {
        if (holdingId == null || vo.getCurrentPrice() == null) {
            return;
        }
        PortfolioHolding entity = new PortfolioHolding();
        entity.setId(holdingId);
        entity.setCachedPrice(vo.getCurrentPrice());
        entity.setCachedPriceTime(LocalDateTime.now());
        if (vo.getTakeProfitPlan() != null) {
            entity.setTakeProfitTrailingPeakPrice(vo.getTakeProfitPlan().getTrailingPeakPrice());
            entity.setTakeProfitTrailingTriggered(vo.getTakeProfitPlan().getTrailingStopTriggered());
        }
        holdingDao.updateById(entity);
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
        long count = holdingDao.countByUserId(userId);
        if (count >= MAX_HOLDING_COUNT) {
            throw new RuntimeException("持仓数量已达上限（" + MAX_HOLDING_COUNT + "个），请先删除部分持仓再添加");
        }

        long existing = holdingDao.countByUserIdAndAccountIdAndStockCode(userId, request.getAccountId(), request.getStockCode());
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
        holdingDao.insert(holding);
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

        long currentCount = holdingDao.countByUserId(userId);

        Set<String> batchCodes = new HashSet<>();
        for (PortfolioHoldingRequest item : items) {
            if (!batchCodes.add(item.getStockCode())) {
                throw new RuntimeException("批量列表中存在重复标的：" + item.getStockCode());
            }

            long existing = holdingDao.countByUserIdAndAccountIdAndStockCode(userId, accountId, item.getStockCode());
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
            holdingDao.insert(holding);
        }
    }

    @Override
    @Transactional
    public void deleteHolding(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        long count = holdingDao.countByIdAndUserId(id, userId);
        if (count == 0) {
            throw new RuntimeException("持仓记录不存在");
        }
        holdingDao.deleteById(id);
    }

    /**
     * 保存用户自己持仓的止盈计划；计划只用于提醒，交易仍由用户自行确认。
     */
    @Override
    @Transactional
    public void saveTakeProfitPlan(Long holdingId, TakeProfitPlanRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (holdingDao.countByIdAndUserId(holdingId, userId) == 0) {
            throw new RuntimeException("持仓记录不存在");
        }
        validateTakeProfitPlan(request);

        PortfolioHolding holding = new PortfolioHolding();
        holding.setId(holdingId);
        holding.setTakeProfitEnabled(Boolean.TRUE.equals(request.getEnabled()));
        holding.setTakeProfitFirstTargetPrice(scalePrice(request.getFirstTargetPrice()));
        holding.setTakeProfitFirstTargetRatio(request.getFirstTargetRatio());
        holding.setTakeProfitSecondTargetPrice(scalePrice(request.getSecondTargetPrice()));
        holding.setTakeProfitSecondTargetRatio(request.getSecondTargetRatio());
        holding.setTakeProfitTrailingDrawdownPercent(request.getTrailingDrawdownPercent());
        holding.setTakeProfitTrailingPeakPrice(null);
        holding.setTakeProfitTrailingTriggered(false);
        holding.setTakeProfitExpiresOn(request.getExpiresOn());
        holdingDao.updateById(holding);
    }

    /**
     * 对未配置计划的持仓应用默认分批止盈方案，已有计划保持不变。
     */
    @Override
    @Transactional
    public int applyDefaultTakeProfitPlans() {
        return holdingDao.initializeDefaultTakeProfitPlans(StpUtil.getLoginIdAsLong());
    }

    private void validateTakeProfitPlan(TakeProfitPlanRequest request) {
        if (!Boolean.TRUE.equals(request.getEnabled())) {
            return;
        }
        if (!request.hasValidTargetRatio()) {
            throw new RuntimeException("前两档止盈累计减仓比例不能超过100%");
        }
        if (request.getFirstTargetPrice() == null && request.getSecondTargetPrice() == null
                && request.getTrailingDrawdownPercent() == null) {
            throw new RuntimeException("请至少设置一个目标价或跟踪止盈条件");
        }
        if (request.getFirstTargetPrice() != null && request.getSecondTargetPrice() != null
                && request.getSecondTargetPrice().compareTo(request.getFirstTargetPrice()) <= 0) {
            throw new RuntimeException("第二目标价必须高于第一目标价");
        }
    }

    private BigDecimal scalePrice(BigDecimal price) {
        return price == null ? null : price.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private TakeProfitPlanVO buildTakeProfitPlan(PortfolioHolding holding, BigDecimal currentPrice) {
        TakeProfitPlanVO plan = new TakeProfitPlanVO();
        plan.setEnabled(holding.getTakeProfitEnabled());
        plan.setFirstTargetPrice(holding.getTakeProfitFirstTargetPrice());
        plan.setFirstTargetRatio(holding.getTakeProfitFirstTargetRatio());
        plan.setSecondTargetPrice(holding.getTakeProfitSecondTargetPrice());
        plan.setSecondTargetRatio(holding.getTakeProfitSecondTargetRatio());
        plan.setTrailingDrawdownPercent(holding.getTakeProfitTrailingDrawdownPercent());
        plan.setTrailingPeakPrice(holding.getTakeProfitTrailingPeakPrice());
        plan.setTrailingStopTriggered(holding.getTakeProfitTrailingTriggered());
        plan.setExpiresOn(holding.getTakeProfitExpiresOn());
        refreshTakeProfitPlanStatus(plan, currentPrice);
        return plan;
    }

    private void refreshTakeProfitPlanStatus(TakeProfitPlanVO plan, BigDecimal currentPrice) {
        TakeProfitPlanStatusResolver.resolve(plan, currentPrice, LocalDate.now());
    }

    /**
     * 获取当前用户所有持仓的股票代码集合
     */
    @Override
    public Set<String> getHoldingStockCodes() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<PortfolioHolding> holdings = holdingDao.selectByUserId(userId);
        return holdings.stream()
                .map(PortfolioHolding::getStockCode)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
