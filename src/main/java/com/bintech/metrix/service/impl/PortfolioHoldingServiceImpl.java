package com.bintech.metrix.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.dto.request.PortfolioHoldingRequest;
import com.bintech.metrix.dto.response.PortfolioHoldingVO;
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

    /** 每次刷新行情最多处理前10只 */
    private static final int MAX_REFRESH_COUNT = 10;
    /** 持仓总数上限 */
    private static final int MAX_HOLDING_COUNT = 100;
    /** 盈亏百分比计算精度（4位小数） */
    private static final int PROFIT_LOSS_SCALE = 4;
    /** 金额统一保留3位小数 */
    private static final int PRICE_SCALE = 3;

    private final PortfolioHoldingMapper holdingMapper;
    private final BrokerAccountMapper accountMapper;
    private final StockBasicService stockBasicService;
    private final MarketDataService marketDataService;

    /** 异步行情刷新缓存 — key=持仓ID, value=已刷新的VO（含实时行情） */
    private final ConcurrentHashMap<Long, PortfolioHoldingVO> priceRefreshCache = new ConcurrentHashMap<>();
    /** 虚拟线程池，异步执行行情拉取，避免阻塞HTTP响应 */
    private final ExecutorService refreshExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @PreDestroy
    public void shutdown() {
        refreshExecutor.close();
    }

    /**
     * 查询持仓列表
     *
     * <ol>
     *   <li>按 {@code accountId} 过滤（为空查全部）</li>
     *   <li>跳过成本或数量为空的记录</li>
     *   <li>按 {@code keyword} 匹配券商/代码/名称（模糊）</li>
     * </ol>
     */
    @Override
    public List<PortfolioHoldingVO> getHoldings(String keyword, Long accountId) {
        LambdaQueryWrapper<PortfolioHolding> queryWrapper = new LambdaQueryWrapper<>();
        if (accountId != null) {
            queryWrapper.eq(PortfolioHolding::getAccountId, accountId);
        }
        List<PortfolioHolding> holdings = holdingMapper.selectList(queryWrapper);
        if (holdings.isEmpty()) {
            return List.of();
        }

        List<BrokerAccount> accounts = accountMapper.selectList(null);
        Map<Long, BrokerAccount> accountMap = accounts.stream()
                .collect(Collectors.toMap(BrokerAccount::getId, a -> a));

        List<PortfolioHoldingVO> vos = new ArrayList<>();
        for (PortfolioHolding h : holdings) {
            if (h.getCost() == null || h.getQuantity() == null) {
                continue;
            }

            PortfolioHoldingVO vo = buildBaseVO(h, accountMap);
            vos.add(vo);
        }

        if (StrUtil.isNotBlank(keyword)) {
            String kw = keyword.trim();
            vos = vos.stream()
                    .filter(v -> (v.getBrokerName() != null && v.getBrokerName().contains(kw))
                            || (v.getStockCode() != null && v.getStockCode().contains(kw))
                            || (v.getStockName() != null && v.getStockName().contains(kw)))
                    .collect(Collectors.toList());
        }

        return vos;
    }

    /**
     * 刷新实时行情（异步）
     *
     * <p>最多取前10条有成本&数量的记录，为每只标的创建异步任务提交到虚拟线程池，
     * 任务完成后的VO会写入 {@link #priceRefreshCache}，供 {@link #pollRefreshedPrices} 轮询消费。
     * 方法立即返回不含行情字段的VO列表。
     */
    @Override
    public List<PortfolioHoldingVO> refreshPrices() {
        List<PortfolioHolding> holdings = holdingMapper.selectList(
                new LambdaQueryWrapper<PortfolioHolding>()
                        .isNotNull(PortfolioHolding::getCost)
                        .isNotNull(PortfolioHolding::getQuantity)
                        .last("LIMIT " + MAX_REFRESH_COUNT));
        if (holdings.isEmpty()) {
            return List.of();
        }

        List<BrokerAccount> accounts = accountMapper.selectList(null);
        Map<Long, BrokerAccount> accountMap = accounts.stream()
                .collect(Collectors.toMap(BrokerAccount::getId, a -> a));

        List<PortfolioHoldingVO> vos = new ArrayList<>();
        for (PortfolioHolding h : holdings) {
            PortfolioHoldingVO vo = buildBaseVO(h, accountMap);
            vos.add(vo);
            refreshExecutor.submit(() -> {
                try {
                    fetchCurrentPrice(vo);
                    priceRefreshCache.put(vo.getId(), vo);
                } catch (Exception e) {
                    log.warn("异步刷新 {} 行情失败: {}", vo.getStockCode(), e.getMessage());
                }
            });
        }
        return vos;
    }

    /**
     * 轮询已刷新完成的实时行情
     *
     * <p>前端定时调用（2s间隔），从 {@link #priceRefreshCache} 中取出已完成的任务。
     * 取出即删除（一次性消费），未被取到的说明仍在处理中。
     *
     * @param ids 待轮询的持仓ID列表
     * @return 已完成刷新的VO映射；key=持仓ID, value=含实时行情的VO
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

    /**
     * 构建基础VO（不含行情字段）
     *
     * @param h          持仓实体
     * @param accountMap 账户ID → 账户实体 映射
     * @return 基础VO
     */
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
        return vo;
    }

    /**
     * 获取实时行情并计算盈亏
     *
     * <p>通过 {@link StockBasicService} 获取股票基础信息，再调用 {@link MarketDataService}
     * 获取实时行情，解析出最新价后设置到VO中，同时计算盈亏金额和盈亏百分比。
     *
     * @param vo 待填充的持仓VO（会被原地修改）
     */
    private void fetchCurrentPrice(PortfolioHoldingVO vo) {
        try {
            StockBasic stockBasic = stockBasicService.getByTsCode(vo.getStockCode());
            if (stockBasic == null) {
                return;
            }
            Map<String, Object> marketData = marketDataService.fetchRealTimeData(stockBasic);
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

    /**
     * 计算盈亏百分比（成本总额大于0时）
     */
    private void calcProfitLossPercent(PortfolioHoldingVO vo, BigDecimal costTotal, BigDecimal plAmount) {
        if (costTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal plPercent = plAmount
                .divide(costTotal, PROFIT_LOSS_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        vo.setProfitLossPercent(plPercent);
    }

    /**
     * 从市场数据中提取最新价
     *
     * <p>市场数据JSON结构：{ status: "success", data: [{ last_price: "xx.xx" }, ...] }
     *
     * @param marketData 原始市场数据
     * @return 最新价（BigDecimal），解析失败返回 null
     */
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

    /**
     * 新增持仓标的
     *
     * <p>校验逻辑：
     * <ol>
     *   <li>全局持仓总数不得超过100</li>
     *   <li>同一账户下不可重复添加同一标的（按 accountId + stockCode 判重）</li>
     * </ol>
     */
    @Override
    @Transactional
    public void createHolding(PortfolioHoldingRequest request) {
        long count = holdingMapper.selectCount(null);
        if (count >= MAX_HOLDING_COUNT) {
            throw new RuntimeException("持仓数量已达上限（" + MAX_HOLDING_COUNT + "个），请先删除部分持仓再添加");
        }

        Long existing = holdingMapper.selectCount(new LambdaQueryWrapper<PortfolioHolding>()
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
        holding.setCreateTime(LocalDateTime.now());
        holding.setUpdateTime(LocalDateTime.now());
        holdingMapper.insert(holding);
    }

    @Override
    @Transactional
    public void deleteHolding(Long id) {
        holdingMapper.deleteById(id);
    }

    @Override
    public Set<String> getHoldingStockCodes() {
        List<PortfolioHolding> holdings = holdingMapper.selectList(null);
        return holdings.stream()
                .map(PortfolioHolding::getStockCode)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
