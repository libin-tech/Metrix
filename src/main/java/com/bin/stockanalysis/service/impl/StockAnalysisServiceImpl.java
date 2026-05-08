package com.bin.stockanalysis.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bin.stockanalysis.dto.request.StockAnalysisRequest;
import com.bin.stockanalysis.dto.response.AiAnalysisResult;
import com.bin.stockanalysis.dto.response.StockAnalysisResponse;
import com.bin.stockanalysis.repository.entity.StockAnalysisRecord;
import com.bin.stockanalysis.repository.mapper.StockAnalysisRecordMapper;
import com.bin.stockanalysis.service.AiModelService;
import com.bin.stockanalysis.service.MarketDataService;
import com.bin.stockanalysis.service.NewsService;
import com.bin.stockanalysis.service.StockAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 股票分析服务实现类
 * 
 * <p>该类是股票分析模块的核心服务，负责整合多维度数据进行股票分析。
 * 主要功能包括：
 * <ul>
 *   <li>执行股票综合分析，生成分析记录</li>
 *   <li>获取仪表盘分析数据，包含技术面、基本面、资金流等</li>
 *   <li>生成核心结论，包含综合评分、风险评估、操作建议</li>
 *   <li>管理分析记录的增删查操作</li>
 *   <li>定时清理过期分析记录</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAnalysisServiceImpl implements StockAnalysisService {

    private final StockAnalysisRecordMapper recordMapper;
    private final MarketDataService marketDataService;
    private final NewsService newsService;
    private final AiModelService aiModelService;


    /**
     * 执行股票分析
     * 
     * <p>根据请求参数执行股票分析，支持多种分析类型（综合分析、基本面分析、技术面分析、情绪分析）。
     * 分析过程包括：获取市场数据、获取新闻摘要、构建分析提示词、调用AI模型生成分析结果、保存分析记录。
     * 
     * @param request 分析请求参数，包含股票代码、分析类型、是否包含市场数据、是否包含新闻分析等
     * @return 分析响应，包含股票代码、名称、分析结果、置信度分数、市场数据、新闻摘要等
     */
    @Override
    @Transactional
    public StockAnalysisResponse analyzeStock(StockAnalysisRequest request) {
        // 1. 提取请求参数：股票代码和分析类型
        String stockCode = request.getStockCode();
        String analysisType = request.getAnalysisType();

        Map<String, Object> marketData = null;
        Map<String, Object> newsSummary = null;

        // 2. 根据请求参数判断是否需要获取市场数据
        //    includeMarketData为true时，调用市场数据服务获取实时行情
        if (request.getIncludeMarketData() != null && request.getIncludeMarketData()) {
            marketData = marketDataService.fetchMarketData(stockCode);
        }

        // 3. 根据请求参数判断是否需要获取新闻数据
        //    includeNews为true时，调用新闻服务获取并汇总新闻
        if (request.getIncludeNews() != null && request.getIncludeNews()) {
            Map<String, Object> newsResult = newsService.fetchStockNews(stockCode);
            // 判断新闻获取是否成功（status为success表示成功）
            if ("success".equals(newsResult.get("status"))) {
                JSONArray newsArray = (JSONArray) newsResult.get("data");
                List<Map<String, Object>> newsList = new ArrayList<>();
                // 遍历新闻数组，提取关键字段：标题、摘要、来源、发布时间
                for (Object obj : newsArray) {
                    JSONObject jsonObj = (JSONObject) obj;
                    Map<String, Object> newsItem = new HashMap<>();
                    newsItem.put("title", jsonObj.getStr("title"));
                    newsItem.put("summary", jsonObj.getStr("summary"));
                    newsItem.put("source", jsonObj.getStr("source"));
                    newsItem.put("publishTime", jsonObj.getStr("publishTime"));
                    newsList.add(newsItem);
                }
                // 构建新闻摘要MAP，包含：新闻数量、汇总内容、新闻列表
                newsSummary = new HashMap<>();
                newsSummary.put("count", newsList.size());
                newsSummary.put("summary", newsService.summarizeNews(newsList));
                newsSummary.put("newsList", newsList);
            }
        }

        // 4. 构建AI分析提示词，包含市场数据和新闻摘要
        String prompt = buildAnalysisPrompt(stockCode, analysisType, marketData, newsSummary);
        // 5. 动态获取当前激活的AI模型类型，从数据库查询（默认OPENAI）
        String modelType = aiModelService.getActiveModelType();
        log.info("使用动态获取的模型类型进行分析: {}", modelType);
        // 6. 调用AI模型生成分析结果（包含置信度）
        AiAnalysisResult aiResult = aiModelService.generateAnalysisWithConfidence(prompt, modelType);

        // 7. 构建分析记录实体，包含股票代码、名称、分析类型、结果等
        StockAnalysisRecord record = new StockAnalysisRecord();
        record.setStockCode(stockCode);
        record.setStockName(getStockNameFromMarketData(marketData));
        record.setAnalysisType(analysisType);
        record.setAnalysisResult(aiResult.getContent());
        record.setConfidenceScore(aiResult.getConfidenceScore()); // 动态计算的置信度
        record.setMarketData(marketData != null ? JSONUtil.toJsonStr(marketData) : null);
        record.setNewsSummary(newsSummary != null ? JSONUtil.toJsonStr(newsSummary) : null);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        // 8. 保存分析记录到数据库
        recordMapper.insert(record);
        // 9. 清理超过200条的旧记录，保持数据量在合理范围
        cleanupExcessRecords();

        // 10. 构建并返回响应对象
        StockAnalysisResponse response = new StockAnalysisResponse();
        response.setStockCode(stockCode);
        response.setStockName(record.getStockName());
        response.setAnalysisType(analysisType);
        response.setAnalysisResult(aiResult.getContent());
        response.setConfidenceScore(aiResult.getConfidenceScore());
        response.setMarketData(marketData);
        response.setNewsSummary(newsSummary);
        response.setCreatedAt(record.getCreateTime());

        return response;
    }



    /**
     * 构建AI分析提示词
     * 
     * <p>根据请求参数构建用于AI模型的分析提示词，包含：
     * <ul>
     *   <li>股票代码和分析类型</li>
     *   <li>市场数据（如有）</li>
     *   <li>新闻摘要（如有）</li>
     *   <li>分析要求</li>
     * </ul>
     * 
     * @param stockCode 股票代码
     * @param analysisType 分析类型
     * @param marketData 市场数据
     * @param newsSummary 新闻摘要
     * @return 格式化后的AI提示词
     */
    private String buildAnalysisPrompt(String stockCode, String analysisType, 
                                       Map<String, Object> marketData, Map<String, Object> newsSummary) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请对股票 ").append(stockCode).append(" 进行").append(analysisType).append("分析。\n\n");
        
        // 添加市场数据（如果可用）
        if (marketData != null && "success".equals(marketData.get("status"))) {
            JSONObject data = (JSONObject) marketData.get("data");
            prompt.append("市场数据：\n");
            prompt.append("开盘价: ").append(data.getBigDecimal("open")).append("\n");
            prompt.append("最高价: ").append(data.getBigDecimal("high")).append("\n");
            prompt.append("最低价: ").append(data.getBigDecimal("low")).append("\n");
            prompt.append("收盘价: ").append(data.getBigDecimal("close")).append("\n");
            prompt.append("成交量: ").append(data.getLong("volume")).append("\n");
            prompt.append("涨跌幅: ").append(data.getBigDecimal("changePercent")).append("%\n\n");
        }
        
        // 添加新闻摘要（如果可用）
        if (newsSummary != null) {
            prompt.append("最新新闻摘要：\n");
            prompt.append(newsSummary.get("summary")).append("\n\n");
        }
        
        // 添加分析要求
        prompt.append("请提供以下分析内容：\n");
        prompt.append("1. 基本面分析\n");
        prompt.append("2. 技术面分析\n");
        prompt.append("3. 市场情绪分析\n");
        prompt.append("4. 投资建议\n");
        prompt.append("5. 风险提示\n");
        
        return prompt.toString();
    }

    /**
     * 从市场数据中提取股票名称
     * 
     * @param marketData 市场数据MAP
     * @return 股票名称，若获取失败则返回"未知"
     */
    private String getStockNameFromMarketData(Map<String, Object> marketData) {
        if (marketData != null && "success".equals(marketData.get("status"))) {
            JSONObject data = (JSONObject) marketData.get("data");
            return data.getStr("name", "未知");
        }
        return "未知";
    }

    @Override
    public StockAnalysisRecord getAnalysisById(Long id) {
        // 使用MyBatis-Plus根据ID查询分析记录
        StockAnalysisRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("Analysis record not found");
        }
        return record;
    }



    @Override
    public List<StockAnalysisRecord> getAllAnalysisRecords() {
        // 查询所有分析记录，不排序
        return recordMapper.selectList(null);
    }


    /**
     * 清理过期分析记录
     * 
     * <p>当分析记录总数超过200条时，删除最旧的记录，只保留最新的200条。
     * 此方法确保数据库中不会积累过多历史分析记录。
     */
    @Override
    @Transactional
    public void cleanupExcessRecords() {
        // 统计当前记录总数
        long total = recordMapper.selectCount(new LambdaQueryWrapper<>());
        // 如果总数未超过200条，无需清理
        if (total <= 200) {
            return;
        }
        // 查询最新的200条记录
        List<StockAnalysisRecord> latestRecords = recordMapper.selectList(
                new LambdaQueryWrapper<StockAnalysisRecord>()
                        .orderByDesc(StockAnalysisRecord::getCreateTime)
                        .last("LIMIT 200"));
        // 提取需要保留的记录ID
        List<Long> keepIds = latestRecords.stream()
                .map(StockAnalysisRecord::getId)
                .collect(Collectors.toList());
        // 删除不在保留列表中的记录
        recordMapper.delete(new LambdaQueryWrapper<StockAnalysisRecord>()
                .notIn(StockAnalysisRecord::getId, keepIds));
    }

    /**
     * 定时清理任务
     * 
     * <p>每天凌晨3点执行一次清理任务，确保分析记录保持合理数量。
     * 使用@Scheduled注解实现定时调度。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void scheduledCleanup() {
        log.info("开始定时清理股票分析记录，仅保留最近200条");
        cleanupExcessRecords();
        log.info("定时清理完成");
    }






}