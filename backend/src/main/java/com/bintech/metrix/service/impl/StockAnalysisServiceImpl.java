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
import com.bintech.metrix.core.analysis.AnalysisOverviewBuilder;
import com.bintech.metrix.core.analysis.AnalysisPromptBuilder;
import com.bintech.metrix.core.analysis.NewsCollector;
import com.bintech.metrix.dto.request.StockAnalysisRequest;
import com.bintech.metrix.dto.response.CursorPageResult;
import com.bintech.metrix.dto.response.NewsItem;
import com.bintech.metrix.dto.response.StockAnalysisDetailResponse;
import com.bintech.metrix.dto.response.StockAnalysisResponse;
import com.bintech.metrix.enums.StockAnalysisStatus;
import com.bintech.metrix.model.AnalysisOverview;
import com.bintech.metrix.repository.entity.StockAnalysisRecord;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.repository.mapper.StockAnalysisRecordMapper;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.NotificationService;
import com.bintech.metrix.service.StockAnalysisService;
import com.bintech.metrix.service.StockBasicService;
import com.bintech.metrix.util.MarkdownRenderer;

import cn.dev33.satoken.stp.StpUtil;
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
     * 执行股票全维度分析
     * <p>按序采集实时行情、五档深度、K线、筹码分布、十大流通股东、新闻舆情，
     * 构建AI提示词并调用模型生成分析报告，最后从报告中提取核心洞察和概览数据。</p>
     */
    @Override
    @Transactional
    public StockAnalysisResponse analyzeStock(StockAnalysisRequest request, StockAnalysisRecord record) {
        String stockCode = request.getStockCode();
        StockBasic stockBasic = stockBasicService.getByTsCode(stockCode);
        String analysisType = request.getAnalysisType();
        Long userId = record.getUserId();

        cleanupExcessRecords();

        String modelType = aiModelService.getActiveModelType(userId);
        log.info("使用动态获取的模型类型进行分析: {}, userId={}", modelType, userId);

        Map<String, Object> marketData = marketDataService.fetchRealTimeData(stockBasic, userId);
        Map<String, Object> depthData = null;
        try {
            depthData = marketDataService.fetchDepthData(stockBasic, userId);
        } catch (Exception e) {
            log.warn("获取五档深度行情失败，跳过: {}", e.getMessage());
        }
        Map<String, Object> klinesData = marketDataService.fetchKlinesData(stockBasic, BusinessConstants.DEFAULT_KLINE_LIMIT, userId);
        Map<String, Object> chipData = marketDataService.fetchChipData(stockBasic, userId);
        Map<String, Object> topFreeShareholdersData = marketDataService.fetchTopFreeShareholdersData(stockBasic, userId);
        Map<String, Object> newsSummary = newsCollector.collect(stockBasic, modelType, userId);

        String prompt = analysisPromptBuilder.build(stockBasic, analysisType, marketData, depthData, klinesData, newsSummary, chipData, topFreeShareholdersData);
        String content = aiModelService.generateAnalysis(prompt, modelType, userId);
        String coreInsight = analysisOverviewBuilder.generateCoreInsight(content, modelType, userId);
        AnalysisOverview overview = analysisOverviewBuilder.build(marketData, klinesData, content, coreInsight, chipData, topFreeShareholdersData);

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

    /**
     * 根据主键查询分析记录，不存在时抛出异常
     */
    @Override
    public StockAnalysisRecord getAnalysisById(Long id) {
        StockAnalysisRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("Analysis record not found");
        }
        return record;
    }

    /**
     * 获取当前用户的所有分析记录，按ID倒序排列
     */
    @Override
    public List<StockAnalysisRecord> getAllAnalysisRecords() {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<StockAnalysisRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StockAnalysisRecord::getUserId, userId);
        queryWrapper.orderByDesc(StockAnalysisRecord::getId);
        return recordMapper.selectList(queryWrapper);
    }

    /**
     * 游标分页查询当前用户的分析记录
     * @param cursor 上一页最后一条的ID，null时从最新开始
     * @param limit 每页条数（实际多取一条用于判断 hasMore）
     */
    @Override
    public CursorPageResult<StockAnalysisRecord> cursorQuery(Long cursor, int limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<StockAnalysisRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockAnalysisRecord::getUserId, userId);
        if (cursor != null && cursor > 0) {
            wrapper.lt(StockAnalysisRecord::getId, cursor);
        }
        wrapper.orderByDesc(StockAnalysisRecord::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<StockAnalysisRecord> records = recordMapper.selectList(wrapper);
        boolean hasMore = records.size() > limit;
        if (hasMore) {
            records = records.subList(0, limit);
        }
        Long nextCursor = records.isEmpty() ? null : records.getLast().getId();
        return CursorPageResult.<StockAnalysisRecord>builder()
                .items(records)
                .hasMore(hasMore)
                .nextCursor(nextCursor)
                .build();
    }

    /**
     * 删除指定分析记录
     */
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

    /**
     * 清理超出 {@link BusinessConstants#MAX_RECORD_KEEP_COUNT} 的旧分析记录，
     * 仅保留最新的 N 条
     */
    @Override
    @Transactional
    public void cleanupExcessRecords() {
        List<StockAnalysisRecord> allRecords = recordMapper.selectList(
                new LambdaQueryWrapper<StockAnalysisRecord>()
                        .orderByDesc(StockAnalysisRecord::getCreateTime));
        if (allRecords.isEmpty()) return;

        long total = allRecords.size();
        if (total <= BusinessConstants.MAX_RECORD_KEEP_COUNT) return;

        List<Long> keepIds = allRecords.stream()
                .limit(BusinessConstants.MAX_RECORD_KEEP_COUNT)
                .map(StockAnalysisRecord::getId)
                .collect(Collectors.toList());
        recordMapper.delete(new LambdaQueryWrapper<StockAnalysisRecord>()
                .notIn(StockAnalysisRecord::getId, keepIds));
    }

    /**
     * 获取分析报告详情，包含渲染后的分析结果、新闻摘要和列表
     */
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

    /**
     * 执行异步分析任务（由任务队列 Worker 调用）
     * 分析完成后根据请求决定是否自动推送到飞书
     */
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

        if (Boolean.TRUE.equals(request.getPushToFeishu())) {
            try {
                pushToFeishu(recordId);
                log.info("分析完成自动推送飞书成功: recordId={}", recordId);
            } catch (Exception e) {
                log.warn("分析完成自动推送飞书失败: recordId={}, error={}", recordId, e.getMessage());
            }
        }
    }

    /**
     * 将已完成的分析报告推送到飞书，包含核心洞察卡片
     * @throws RuntimeException 当记录未完成或推送失败时抛出
     */
    @Override
    public void pushToFeishu(Long id) {
        StockAnalysisRecord record = getAnalysisById(id);
        if (record.getStatus() != StockAnalysisStatus.COMPLETED) {
            throw new RuntimeException("分析记录未完成，无法推送");
        }

        StockBasic stockBasic = stockBasicService.getByTsCode(record.getStockCode());

        String overviewJson = record.getAnalysisOverview();
        String coreInsight = "";
        if (overviewJson != null) {
            try {
                JSONObject overview = JSONUtil.parseObj(overviewJson);
                coreInsight = overview.getStr("coreInsight", "");
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
                coreInsight, overviewJson, analysisTime, record.getUserId());

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
