package com.bintech.metrix.service;

import com.bintech.metrix.dto.request.PortfolioHoldingRequest;
import com.bintech.metrix.dto.response.PortfolioHoldingVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 持仓管理服务接口
 *
 * <p>提供持仓标的的增删查、行情刷新（异步+轮询）等功能。
 */
public interface PortfolioHoldingService {

    /**
     * 查询持仓列表（支持关键字搜索和账户过滤）
     *
     * @param keyword   搜索关键字，匹配券商名称、标的代码、标的名称
     * @param accountId 账户ID，为空则查询所有账户
     * @return 持仓VO列表
     */
    List<PortfolioHoldingVO> getHoldings(String keyword, Long accountId);

    /**
     * 刷新实时行情（异步）
     *
     * <p>查询有成本&数量的前10只标的，将每只标的的行情获取提交到虚拟线程池异步执行，
     * 完成后结果存入 {@code priceRefreshCache} 供轮询消费。立即返回持仓VO列表（行情字段为空）。
     *
     * @return 持仓VO列表（不含实时行情）
     */
    List<PortfolioHoldingVO> refreshPrices();

    /**
     * 轮询已刷新完成的实时行情
     *
     * <p>前端定时调用，传入待刷新的持仓ID列表，服务端从缓存中取出已完成的任务结果并返回，
     * 取出的记录会从缓存中移除。
     *
     * @param ids 待轮询的持仓ID列表
     * @return 已完成刷新的持仓VO映射（key=持仓ID）
     */
    Map<Long, PortfolioHoldingVO> pollRefreshedPrices(List<Long> ids);

    /**
     * 新增持仓标的
     *
     * @param request 持仓请求（账户、标的代码、名称、成本、数量）
     */
    void createHolding(PortfolioHoldingRequest request);

    /**
     * 批量新增持仓标的（同一账户下）
     *
     * @param accountId 账户ID
     * @param items     持仓请求列表
     */
    void batchCreateHoldings(Long accountId, List<PortfolioHoldingRequest> items);

    /**
     * 删除持仓标的
     *
     * @param id 持仓ID
     */
    void deleteHolding(Long id);

    /**
     * 获取所有持仓标的的代码集合
     *
     * @return 持仓标的代码集合
     */
    Set<String> getHoldingStockCodes();
}
