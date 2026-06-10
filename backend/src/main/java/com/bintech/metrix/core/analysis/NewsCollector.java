package com.bintech.metrix.core.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.NewsService;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollector {

    private final NewsService newsService;
    private final AiModelService aiModelService;
    private final NewsPromptBuilder newsPromptBuilder;

    /**
     * 获取股票相关新闻并生成AI摘要
     *
     * @param stockBasic 股票基本信息
     * @param modelType  AI模型类型
     * @return 新闻摘要Map，包含 count / summary / newsList 三个字段；失败返回null
     */
    public Map<String, Object> collect(StockBasic stockBasic, String modelType) {
        return collect(stockBasic, modelType, null);
    }

    /**
     * 获取股票相关新闻并生成AI摘要
     *
     * @param stockBasic 股票基本信息
     * @param modelType  AI模型类型
     * @param userId     用户ID
     * @return 新闻摘要Map，包含 count / summary / newsList 三个字段；失败返回null
     */
    public Map<String, Object> collect(StockBasic stockBasic, String modelType, Long userId) {
        Map<String, Object> newsResult = newsService.fetchStockNews(stockBasic, userId);

        if (!ApiConstants.STATUS_SUCCESS.equals(newsResult.get(ApiConstants.KEY_STATUS))) {
            String errorMsg = (String) newsResult.get(ApiConstants.KEY_MESSAGE);
            log.warn("获取新闻失败: {}", errorMsg);
            return null;
        }

        Object dataObj = newsResult.get("data");
        if (!(dataObj instanceof JSONArray newsArray)) {
            log.warn("新闻数据的data字段不是JSONArray类型");
            return null;
        }

        List<Map<String, Object>> newsList = new ArrayList<>();
        for (int i = 0; i < newsArray.size(); i++) {
            try {
                JSONObject jsonObj = newsArray.getJSONObject(i);
                Map<String, Object> newsItem = new HashMap<>();
                newsItem.put(ApiConstants.KEY_TITLE, jsonObj.getStr(ApiConstants.KEY_TITLE, "未知标题"));
                newsItem.put(BusinessConstants.KEY_SUMMARY, jsonObj.getStr(BusinessConstants.KEY_SUMMARY, "无摘要"));
                newsItem.put(ApiConstants.KEY_SOURCE, jsonObj.getStr(ApiConstants.KEY_SOURCE, "未知来源"));
                newsItem.put("publishTime", jsonObj.getStr("publishTime", "未知时间"));
                newsItem.put(ApiConstants.KEY_URL, jsonObj.getStr(ApiConstants.KEY_URL, ""));
                newsList.add(newsItem);
            } catch (Exception e) {
                log.warn("解析第{}条新闻数据失败: {}", i + 1, e.getMessage());
            }
        }

        Map<String, Object> newsSummaryMap = new HashMap<>();
        newsSummaryMap.put("count", newsList.size());

        if (!newsList.isEmpty()) {
            String summary = aiModelService.generateAnalysis(
                    newsPromptBuilder.buildSummarizePrompt(newsList), modelType, userId);
            newsSummaryMap.put("summary", summary);
        } else {
            newsSummaryMap.put("summary", "暂无相关新闻");
        }
        newsSummaryMap.put("newsList", newsList);

        log.info("成功获取{}条新闻", newsList.size());
        return newsSummaryMap;
    }

}
