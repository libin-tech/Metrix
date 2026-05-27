package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.bintech.metrix.dto.request.BrokerAccountRequest;
import com.bintech.metrix.dto.request.PortfolioHoldingRequest;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.PortfolioHoldingVO;
import com.bintech.metrix.repository.entity.BrokerAccount;
import com.bintech.metrix.service.BrokerAccountService;
import com.bintech.metrix.service.PortfolioHoldingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 持仓管理控制器
 *
 * <p>提供券商账户和持仓标的的CRUD、行情异步刷新（轮询）REST API。
 * 所有接口均需登录认证（{@code @SaCheckLogin}）。
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@SaCheckLogin
public class PortfolioController {

    private final BrokerAccountService brokerAccountService;
    private final PortfolioHoldingService portfolioHoldingService;

    /**
     * 获取所有券商账户
     *
     * @return 券商账户列表
     */
    @GetMapping("/accounts")
    @SaCheckPermission("portfolio:account:list")
    public ApiResponse<List<BrokerAccount>> getAccounts() {
        return ApiResponse.success(brokerAccountService.getAllAccounts());
    }

    /**
     * 创建券商账户
     *
     * @param request 券商账户请求
     * @return 创建成功的账户对象
     */
    @PostMapping("/accounts")
    @SaCheckPermission("portfolio:account:create")
    public ApiResponse<BrokerAccount> createAccount(@Valid @RequestBody BrokerAccountRequest request) {
        BrokerAccount account = brokerAccountService.createAccount(request);
        return ApiResponse.success("券商账户创建成功", account);
    }

    /**
     * 更新券商账户
     *
     * @param id      账户ID
     * @param request 券商账户请求
     * @return 更新后的账户
     */
    @PutMapping("/accounts/{id}")
    @SaCheckPermission("portfolio:account:update")
    public ApiResponse<BrokerAccount> updateAccount(@PathVariable Long id, @Valid @RequestBody BrokerAccountRequest request) {
        BrokerAccount account = brokerAccountService.updateAccount(id, request);
        return ApiResponse.success("券商账户更新成功", account);
    }

    /**
     * 删除券商账户（会级联删除该账户下所有持仓）
     *
     * @param id 账户ID
     */
    @DeleteMapping("/accounts/{id}")
    @SaCheckPermission("portfolio:account:delete")
    public ApiResponse<Void> deleteAccount(@PathVariable Long id) {
        brokerAccountService.deleteAccount(id);
        return ApiResponse.success("券商账户删除成功", null);
    }

    /**
     * 查询持仓列表
     *
     * @param keyword   搜索关键字（券商名称/标的代码/标的名称）
     * @param accountId 账户ID（为空查全部）
     * @return 持仓VO列表（不含实时行情）
     */
    @GetMapping("/holdings")
    @SaCheckPermission("portfolio:holding:list")
    public ApiResponse<List<PortfolioHoldingVO>> getHoldings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long accountId) {
        return ApiResponse.success(portfolioHoldingService.getHoldings(keyword, accountId));
    }

    /**
     * 新增持仓标的
     *
     * @param request 持仓请求（账户、标的代码/名称、成本、数量）
     */
    @PostMapping("/holdings")
    @SaCheckPermission("portfolio:holding:create")
    public ApiResponse<Void> createHolding(@Valid @RequestBody PortfolioHoldingRequest request) {
        portfolioHoldingService.createHolding(request);
        return ApiResponse.success("持仓添加成功", null);
    }

    /**
     * 批量新增持仓标的（同一账户下）
     *
     * @param accountId 账户ID
     * @param items     持仓请求列表
     */
    @PostMapping("/holdings/batch")
    @SaCheckPermission("portfolio:holding:batch-create")
    public ApiResponse<Void> batchCreateHoldings(
            @RequestParam Long accountId,
            @RequestBody List<PortfolioHoldingRequest> items) {
        portfolioHoldingService.batchCreateHoldings(accountId, items);
        return ApiResponse.success("批量添加成功", null);
    }

    /**
     * 刷新实时行情（异步）
     *
     * <p>最多处理前10只标的，行情获取在虚拟线程中异步执行，
     * 前端需配合 {@link #pollRefreshedPrices} 轮询获取结果。
     *
     * @return 持仓VO列表（实时行情字段为空，需轮询获取）
     */
    @PostMapping("/holdings/refresh-prices")
    @SaCheckPermission("portfolio:holding:refresh")
    public ApiResponse<List<PortfolioHoldingVO>> refreshPrices() {
        return ApiResponse.success(portfolioHoldingService.refreshPrices());
    }

    /**
     * 轮询已刷新完成的实时行情
     *
     * <p>前端定时调用此接口获取已完成异步刷新的行情数据，
     * 每次调用会消费掉已完成的任务结果（缓存中移除）。
     *
     * @param ids 待轮询的持仓ID列表
     * @return 已完成刷新的VO映射（key=持仓ID）
     */
    @PostMapping("/holdings/poll-refreshed")
    @SaCheckPermission("portfolio:holding:poll")
    public ApiResponse<Map<Long, PortfolioHoldingVO>> pollRefreshedPrices(@RequestBody List<Long> ids) {
        return ApiResponse.success(portfolioHoldingService.pollRefreshedPrices(ids));
    }

    /**
     * 删除持仓标的
     *
     * @param id 持仓ID
     */
    @DeleteMapping("/holdings/{id}")
    @SaCheckPermission("portfolio:holding:delete")
    public ApiResponse<Void> deleteHolding(@PathVariable Long id) {
        portfolioHoldingService.deleteHolding(id);
        return ApiResponse.success("持仓删除成功", null);
    }
}
