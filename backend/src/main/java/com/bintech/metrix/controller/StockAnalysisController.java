package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.annotation.CheckConfig;
import com.bintech.metrix.core.queue.AnalysisTask;
import com.bintech.metrix.core.queue.AnalysisTaskQueue;
import com.bintech.metrix.dto.request.StockAnalysisRequest;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.CursorPageResult;
import com.bintech.metrix.dto.response.StockAnalysisDetailResponse;
import com.bintech.metrix.enums.ConfigType;
import com.bintech.metrix.enums.StockAnalysisStatus;
import com.bintech.metrix.repository.dao.StockAnalysisRecordDao;
import com.bintech.metrix.repository.entity.StockAnalysisRecord;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.PortfolioHoldingService;
import com.bintech.metrix.service.StockAnalysisService;
import com.bintech.metrix.service.StockBasicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 股票分析控制器
 * 
 * <p>提供股票分析相关的REST API接口，包括：
 * <ul>
 *   <li>分析记录管理：创建、查询、删除分析记录</li>
 *   <li>仪表盘分析：获取多维度分析数据</li>
 *   <li>核心结论：获取综合评分和投资建议</li>
 *   <li>各维度分析数据：技术面、基本面、资金流、舆情等</li>
 * </ul>
 * 
 * <p>所有接口均需要登录认证（@SaCheckLogin）。
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@SaCheckLogin
public class StockAnalysisController {

    private final StockAnalysisService stockAnalysisService;
    private final StockAnalysisRecordDao recordDao;
    private final StockBasicService stockBasicService;
    private final AnalysisTaskQueue analysisTaskQueue;
    private final PortfolioHoldingService portfolioHoldingService;

    /**
     * 异步执行股票分析
     * 
     * <p>创建分析任务并立即返回，分析任务将在后台异步执行。
     * 最多同时执行3个任务，超出限制的任务将进入等待队列。
     * 
     * @param request 分析请求参数
     * @return 分析任务创建结果
     */
    @PostMapping
    @SaCheckPermission("analysis:record:create")
    @CheckConfig(required = {ConfigType.AI_MODEL, ConfigType.MARKET_DATA, ConfigType.NEWS_SOURCE})
    public ApiResponse<Map<String, Object>> analyzeStockAsync(@Valid @RequestBody StockAnalysisRequest request) {
        String stockCode = request.getStockCode();

        Long userId = StpUtil.getLoginIdAsLong();

        if (!analysisTaskQueue.canSubmit()) {
            return ApiResponse.error("当前任务队列已满，请稍后再试");
        }

        try {
            StockBasic stockBasic = stockBasicService.getByTsCode(stockCode);

            // 检查该股票是否已在分析中
            Long analyzingCount = recordDao.countByStockCodeAndStatus(stockCode, StockAnalysisStatus.ANALYZING);
            if (analyzingCount > 0) {
                Map<String, Object> result = new HashMap<>();
                result.put("stockCode", stockCode);
                result.put("stockName", stockBasic.getName());
                result.put("message", "已添加到分析列表中，耐心等待");
                return ApiResponse.success("已添加到分析列表中，耐心等待", result);
            }

            // 创建分析记录，初始状态为"分析中"
            StockAnalysisRecord record = new StockAnalysisRecord();
            record.setUserId(userId);
            record.setStockCode(stockBasic.getTsCode());
            record.setStockName(stockBasic.getName());
            record.setAnalysisType(request.getAnalysisType());
            record.setStatus(StockAnalysisStatus.ANALYZING);
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            
            recordDao.insert(record);
            
            // 构建任务并提交到队列，由后台worker异步执行
            AnalysisTask task = new AnalysisTask(record.getId(), request, stockBasic.getName(), userId);
            analysisTaskQueue.submit(task);
            
            // 返回任务创建结果给前端轮询
            Map<String, Object> result = new HashMap<>();
            result.put("recordId", record.getId());
            result.put("stockCode", stockCode);
            result.put("stockName", stockBasic.getName());
            result.put("status", "分析中");
            result.put("message", "分析任务已创建，正在排队处理");
            
            return ApiResponse.success("分析任务已创建", result);
            
        } catch (Exception e) {
            return ApiResponse.error("创建分析任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务队列状态
     * 
     * @return 任务队列状态信息
     */
    @GetMapping("/queue/status")
    @SaCheckPermission("analysis:queue:status")
    public ApiResponse<Map<String, Object>> getQueueStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("runningTasks", analysisTaskQueue.getRunningTaskCount());
        status.put("pendingTasks", analysisTaskQueue.getPendingTaskCount());
        status.put("maxConcurrentTasks", analysisTaskQueue.getMaxConcurrentTasks());
        status.put("canSubmit", analysisTaskQueue.canSubmit());
        
        return ApiResponse.success(status);
    }

    /**
     * 获取所有分析记录
     * 
     * @return 分析记录列表
     */
    @GetMapping
    @SaCheckPermission("analysis:record:list")
    public ApiResponse<List<StockAnalysisRecord>> getAllAnalysisRecords() {
        List<StockAnalysisRecord> records = stockAnalysisService.getAllAnalysisRecords();
        Set<String> holdingStockCodes = portfolioHoldingService.getHoldingStockCodes();
        for (StockAnalysisRecord record : records) {
            record.setIsHolding(holdingStockCodes.contains(record.getStockCode()));
        }
        return ApiResponse.success(records);
    }

    /**
     * 游标分页查询分析记录
     *
     * @param cursor 上一页最后一条的ID，缺省为第一页
     * @param limit  每页条数，缺省10
     * @return 游标分页结果
     */
    @GetMapping("/cursor")
    @SaCheckPermission("analysis:record:cursor")
    public ApiResponse<CursorPageResult<StockAnalysisRecord>> cursorQuery(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int limit) {
        CursorPageResult<StockAnalysisRecord> result = stockAnalysisService.cursorQuery(cursor, limit);
        Set<String> holdingStockCodes = portfolioHoldingService.getHoldingStockCodes();
        for (StockAnalysisRecord record : result.getItems()) {
            record.setIsHolding(holdingStockCodes.contains(record.getStockCode()));
        }
        return ApiResponse.success(result);
    }

    /**
     * 根据ID获取分析记录
     * 
     * @param id 分析记录ID
     * @return 分析记录
     */
    @GetMapping("/{id}")
    @SaCheckPermission("analysis:record:detail")
    public ApiResponse<StockAnalysisRecord> getAnalysisById(@PathVariable Long id) {
        StockAnalysisRecord record = stockAnalysisService.getAnalysisById(id);
        return ApiResponse.success(record);
    }

    /**
     * 删除分析记录
     * 
     * @param id 分析记录ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("analysis:record:delete")
    public ApiResponse<Void> deleteAnalysisRecord(@PathVariable Long id) {
        stockAnalysisService.deleteAnalysisRecord(id);
        return ApiResponse.success("分析记录删除成功", null);
    }

    /**
     * 获取分析记录详情（Markdown格式）
     * 
     * <p>返回格式化的分析记录详情，支持Markdown格式渲染，包含新闻列表的用户友好显示。
     * 
     * @param id 分析记录ID
     * @return 格式化的分析详情响应
     */
    @GetMapping("/{id}/detail")
    @SaCheckPermission("analysis:record:report")
    public ApiResponse<StockAnalysisDetailResponse> getAnalysisDetail(@PathVariable Long id) {
        StockAnalysisDetailResponse response = stockAnalysisService.getAnalysisDetail(id);
        return ApiResponse.success(response);
    }

    /**
     * 手动推送分析概览到飞书
     *
     * @param id 分析记录ID
     * @return 推送结果
     */
    @PostMapping("/{id}/push-feishu")
    @SaCheckPermission("analysis:record:push-feishu")
    public ApiResponse<Boolean> pushToFeishu(@PathVariable Long id) {
        try {
            stockAnalysisService.pushToFeishu(id);
            return ApiResponse.success("飞书推送成功", true);
        } catch (Exception e) {
            return ApiResponse.error("飞书推送失败: " + e.getMessage());
        }
    }

}
