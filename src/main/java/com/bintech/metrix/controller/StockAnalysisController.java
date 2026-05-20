package com.bintech.metrix.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.enums.StockAnalysisStatus;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bintech.metrix.dto.request.StockAnalysisRequest;
import com.bintech.metrix.core.queue.AnalysisTask;
import com.bintech.metrix.core.queue.AnalysisTaskQueue;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.StockAnalysisDetailResponse;
import com.bintech.metrix.repository.entity.StockAnalysisRecord;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.repository.mapper.StockAnalysisRecordMapper;
import com.bintech.metrix.service.NotificationService;
import com.bintech.metrix.service.PdfExportService;
import com.bintech.metrix.service.PortfolioHoldingService;
import com.bintech.metrix.service.StockAnalysisService;
import com.bintech.metrix.service.StockBasicService;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final StockAnalysisRecordMapper recordMapper;
    private final StockBasicService stockBasicService;
    private final AnalysisTaskQueue analysisTaskQueue;
    private final NotificationService notificationService;
    private final PdfExportService pdfExportService;
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
    public ApiResponse<Map<String, Object>> analyzeStockAsync(@Valid @RequestBody StockAnalysisRequest request) {
        String stockCode = request.getStockCode();
        
        // 检查队列是否还有容量
        if (!analysisTaskQueue.canSubmit()) {
            return ApiResponse.error("当前任务队列已满，请稍后再试");
        }

        try {
            // 查询股票基本信息
            StockBasic stockBasic = stockBasicService.getByTsCode(stockCode);

            // 检查该股票是否已在分析中
            Long analyzingCount = recordMapper.selectCount(
                    new LambdaQueryWrapper<StockAnalysisRecord>()
                            .eq(StockAnalysisRecord::getStockCode, stockCode)
                            .eq(StockAnalysisRecord::getStatus, StockAnalysisStatus.ANALYZING));
            if (analyzingCount > 0) {
                Map<String, Object> result = new HashMap<>();
                result.put("stockCode", stockCode);
                result.put("stockName", stockBasic.getName());
                result.put("message", "已添加到分析列表中，耐心等待");
                return ApiResponse.success("已添加到分析列表中，耐心等待", result);
            }

            // 创建分析记录，初始状态为"分析中"
            StockAnalysisRecord record = new StockAnalysisRecord();
            record.setStockCode(stockBasic.getTsCode());
            record.setStockName(stockBasic.getName());
            record.setAnalysisType(request.getAnalysisType());
            record.setStatus(StockAnalysisStatus.ANALYZING);
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            
            recordMapper.insert(record);
            
            // 构建任务并提交到队列，由后台worker异步执行
            AnalysisTask task = new AnalysisTask(record.getId(), request, stockBasic.getName());
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
    public ApiResponse<List<StockAnalysisRecord>> getAllAnalysisRecords() {
        List<StockAnalysisRecord> records = stockAnalysisService.getAllAnalysisRecords();
        Set<String> holdingStockCodes = portfolioHoldingService.getHoldingStockCodes();
        for (StockAnalysisRecord record : records) {
            record.setIsHolding(holdingStockCodes.contains(record.getStockCode()));
        }
        return ApiResponse.success(records);
    }

    /**
     * 根据ID获取分析记录
     * 
     * @param id 分析记录ID
     * @return 分析记录
     */
    @GetMapping("/{id}")
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
    public ApiResponse<Boolean> pushToFeishu(@PathVariable Long id) {
        try {
            stockAnalysisService.pushToFeishu(id);
            return ApiResponse.success("飞书推送成功", true);
        } catch (Exception e) {
            return ApiResponse.error("飞书推送失败: " + e.getMessage());
        }
    }

    /**
     * 导出分析报告为PDF
     *
     * @param id 分析记录ID
     * @return PDF文件流
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> exportPdf(@PathVariable Long id) {
        StockAnalysisRecord record = stockAnalysisService.getAnalysisById(id);
        StockBasic stockBasic = stockBasicService.getByTsCode(record.getStockCode());
        if (stockBasic == null) {
            throw new RuntimeException("股票代码 " + record.getStockCode() + " 不存在");
        }

        byte[] pdfBytes = pdfExportService.generatePdf(record, stockBasic);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        String filename = stockBasic.getName() + "（" + stockBasic.getTsCode() + "）深度综合分析报告-" + timestamp + ".pdf";

        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);

    }
}