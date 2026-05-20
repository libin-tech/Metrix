package com.bintech.metrix.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.dto.request.StockAnalysisRequest;
import com.bintech.metrix.dto.response.NewsItem;
import com.bintech.metrix.dto.response.StockAnalysisDetailResponse;
import com.bintech.metrix.dto.response.StockAnalysisResponse;
import com.bintech.metrix.enums.StockAnalysisStatus;
import com.bintech.metrix.model.AnalysisOverview;
import com.bintech.metrix.repository.entity.StockAnalysisRecord;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.repository.mapper.StockAnalysisRecordMapper;
import com.bintech.metrix.core.analysis.AnalysisOverviewBuilder;
import com.bintech.metrix.core.analysis.AnalysisPromptBuilder;
import com.bintech.metrix.core.analysis.NewsCollector;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.NotificationService;
import com.bintech.metrix.service.StockAnalysisService;
import com.bintech.metrix.service.StockBasicService;
import com.bintech.metrix.util.MarkdownRenderer;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAnalysisServiceImpl implements StockAnalysisService {

    private final StockAnalysisRecordMapper recordMapper;
    private final StockBasicService stockBasicService;
    private final MarketDataService marketDataService;
    private final AiModelService aiModelService;
    private final NewsCollector newsCollector;
    private final AnalysisPromptBuilder analysisPromptBuilder;
    private final AnalysisOverviewBuilder analysisOverviewBuilder;
    private final NotificationService notificationService;

    /**
     * 执行完整的股票分析流程：
     * <ol>
     *   <li>获取行情、深度、K线、舆情数据</li>
     *   <li>构建AI提示词并调用模型分析</li>
     *   <li>生成核心洞察与分析概览</li>
     *   <li>持久化分析结果</li>
     * </ol>
     *
     * @param request 分析请求
     * @param record  待更新的分析记录
     * @return 分析响应
     */
    @Override
    @Transactional
    public StockAnalysisResponse analyzeStock(StockAnalysisRequest request, StockAnalysisRecord record) {
        // 解析请求参数，获取股票基本信息
        String stockCode = request.getStockCode();
        StockBasic stockBasic = stockBasicService.getByTsCode(stockCode);
        String analysisType = request.getAnalysisType();

        // 保留最近50条，超出则清理
        cleanupExcessRecords();

        // 获取当前活跃的AI模型类型
        String modelType = aiModelService.getActiveModelType();
        log.info("使用动态获取的模型类型进行分析: {}", modelType);

        // 获取原始数据：实时行情、五档深度、K线、新闻舆情、筹码分布
        Map<String, Object> marketData = marketDataService.fetchRealTimeData(stockBasic);
        Map<String, Object> depthData = marketDataService.fetchDepthData(stockBasic);
        Map<String, Object> klinesData = marketDataService.fetchKlinesData(stockBasic, BusinessConstants.DEFAULT_KLINE_LIMIT);
        Map<String, Object> newsSummary = newsCollector.collect(stockBasic, modelType);
        Map<String, Object> chipData = marketDataService.fetchChipData(stockBasic);

        // AI分析：构建提示词 → 模型生成报告 → 提取核心洞察和关联板块 → 构建分析概览
        String prompt = analysisPromptBuilder.build(stockBasic, analysisType, marketData, depthData, klinesData, newsSummary, chipData);
        String content = aiModelService.generateAnalysis(prompt, modelType);
        String coreInsight = analysisOverviewBuilder.generateCoreInsight(content, modelType);
        AnalysisOverview overview = analysisOverviewBuilder.build(marketData, klinesData, content, coreInsight, chipData);



        // 持久化分析结果
        record.setAnalysisResult(content);
        record.setAnalysisOverview(JSONUtil.toJsonStr(overview));
        record.setMarketData(marketData != null ? JSONUtil.toJsonStr(marketData) : null);
        record.setDepthData(depthData != null ? JSONUtil.toJsonStr(depthData) : null);
        record.setKlinesData(klinesData != null ? JSONUtil.toJsonStr(klinesData) : null);
        record.setNewsSummary(newsSummary != null ? JSONUtil.toJsonStr(newsSummary) : null);
        record.setUpdateTime(LocalDateTime.now());
        record.setStatus(StockAnalysisStatus.COMPLETED);

        recordMapper.updateById(record);
        cleanupExcessRecords();

        // 组装响应
        StockAnalysisResponse response = new StockAnalysisResponse();
        response.setStockCode(stockCode);
        response.setStockName(record.getStockName());
        response.setAnalysisType(analysisType);
        response.setAnalysisResult(content);
        response.setMarketData(marketData);
        response.setNewsSummary(newsSummary);
        response.setCreatedAt(record.getCreateTime());

        return response;
    }

    @Override
    public StockAnalysisRecord getAnalysisById(Long id) {
        StockAnalysisRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("Analysis record not found");
        }
        return record;
    }

    @Override
    public List<StockAnalysisRecord> getAllAnalysisRecords() {
        LambdaQueryWrapper<StockAnalysisRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(StockAnalysisRecord::getId);
        return recordMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public void deleteAnalysisRecord(Long id) {
        StockAnalysisRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("分析记录不存在");
        }
        recordMapper.deleteById(id);
        log.info("分析记录删除成功，ID: {}", id);
    }

    @Override
    @Transactional
    public void cleanupExcessRecords() {
        long total = recordMapper.selectCount(new LambdaQueryWrapper<>());
        if (total <= BusinessConstants.MAX_RECORD_KEEP_COUNT) return;

        List<StockAnalysisRecord> latestRecords = recordMapper.selectList(
                new LambdaQueryWrapper<StockAnalysisRecord>()
                        .orderByDesc(StockAnalysisRecord::getCreateTime)
                        .last("LIMIT " + BusinessConstants.MAX_RECORD_KEEP_COUNT));
        List<Long> keepIds = latestRecords.stream()
                .map(StockAnalysisRecord::getId)
                .collect(Collectors.toList());
        recordMapper.delete(new LambdaQueryWrapper<StockAnalysisRecord>()
                .notIn(StockAnalysisRecord::getId, keepIds));
    }

    @Override
    public StockAnalysisDetailResponse getAnalysisDetail(Long id) {
        StockAnalysisRecord record = getAnalysisById(id);

        return StockAnalysisDetailResponse.builder()
                .id(record.getId())
                .stockCode(record.getStockCode())
                .stockName(record.getStockName())
                .analysisType(record.getAnalysisType())
                .analysisResult(MarkdownRenderer.renderAnalysisResult(record.getAnalysisResult()))
                .newsSummary(parseNewsSummary(record.getNewsSummary()))
                .newsList(parseNewsList(record.getNewsSummary()))
                .createdAt(record.getCreateTime())
                .updatedAt(record.getUpdateTime())
                .analysisOverview(record.getAnalysisOverview())
                .build();
    }

    @Override
    @Transactional
    public void executeAnalysis(Long recordId, StockAnalysisRequest request) {
        log.info("开始执行异步分析任务: recordId={}", recordId);
        StockAnalysisRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            log.error("分析记录不存在: recordId={}", recordId);
            return;
        }
        analyzeStock(request, record);

        // 自动推送到飞书
        if (Boolean.TRUE.equals(request.getPushToFeishu())) {
            try {
                pushToFeishu(recordId);
                log.info("分析完成自动推送飞书成功: recordId={}", recordId);
            } catch (Exception e) {
                log.warn("分析完成自动推送飞书失败: recordId={}, error={}", recordId, e.getMessage());
            }
        }
    }

    @Override
    public void pushToFeishu(Long id) {
        StockAnalysisRecord record = getAnalysisById(id);
        if (record.getStatus() != StockAnalysisStatus.COMPLETED) {
            throw new RuntimeException("分析记录未完成，无法推送");
        }

        StockBasic stockBasic = stockBasicService.getByTsCode(record.getStockCode());

        // 解析概览
        String overviewJson = record.getAnalysisOverview();
        String coreInsight = "";
        if (overviewJson != null) {
            try {
                JSONObject overview = JSONUtil.parseObj(overviewJson);
                coreInsight = overview.getStr("coreInsight", "");
                // 清理Markdown标记用于纯文本展示
                coreInsight = coreInsight.replaceAll("\\*\\*", "").replaceAll("\\*", "");
            } catch (Exception e) {
                log.warn("解析概览JSON失败: {}", e.getMessage());
            }
        }

        String analysisTime = record.getCreateTime() != null
                ? record.getCreateTime().format(java.time.format.DateTimeFormatter.ofPattern(BusinessConstants.DATE_TIME_FORMAT))
                : "";

        boolean success = notificationService.sendFeishuCardMessage(
                stockBasic.getName(), stockBasic.getTsCode(),
                coreInsight, overviewJson, analysisTime);

        if (!success) {
            throw new RuntimeException("飞书推送失败，请检查飞书配置");
        }
    }

    private String parseNewsSummary(String newsSummaryJson) {
        if (newsSummaryJson == null || newsSummaryJson.isEmpty()) return null;
        try {
            return JSONUtil.parseObj(newsSummaryJson).getStr("summary", null);
        } catch (Exception e) {
            log.warn("解析新闻摘要失败: {}", e.getMessage());
            return null;
        }
    }

    private List<NewsItem> parseNewsList(String newsSummaryJson) {
        List<NewsItem> newsList = new ArrayList<>();
        if (newsSummaryJson == null || newsSummaryJson.isEmpty()) return newsList;

        try {
            JSONObject newsSummary = JSONUtil.parseObj(newsSummaryJson);
            JSONArray newsArray = newsSummary.getJSONArray("newsList");
            if (newsArray == null || newsArray.isEmpty()) return newsList;

            for (int i = 0; i < newsArray.size(); i++) {
                try {
                    JSONObject obj = newsArray.getJSONObject(i);
                    newsList.add(NewsItem.builder()
                            .title(obj.getStr("title", "未知标题"))
                            .summary(obj.getStr("summary", ""))
                            .source(obj.getStr("source", "未知来源"))
                            .publishTime(obj.getStr("publishTime", "未知时间"))
                            .url(obj.getStr("url", ""))
                            .build());
                } catch (Exception e) {
                    log.warn("解析第{}条新闻失败: {}", i + 1, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("解析新闻列表失败: {}", e.getMessage());
        }
        return newsList;
    }
}
