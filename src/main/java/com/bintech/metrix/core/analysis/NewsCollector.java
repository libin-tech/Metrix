package com.bintech.metrix.core.analysis;

import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.NewsService;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollector {

    private final NewsService newsService;
    private final AiModelService aiModelService;

    /**
     * 获取股票相关新闻并生成AI摘要
     *
     * @param stockBasic 股票基本信息
     * @param modelType  AI模型类型
     * @return 新闻摘要Map，包含 count / summary / newsList 三个字段；失败返回null
     */
    public Map<String, Object> collect(StockBasic stockBasic, String modelType) {
        // 调用新闻服务获取原始新闻数据
        Map<String, Object> newsResult = newsService.fetchStockNews(stockBasic);

        // 校验接口状态
        if (!ApiConstants.STATUS_SUCCESS.equals(newsResult.get(ApiConstants.KEY_STATUS))) {
            String errorMsg = (String) newsResult.get("message");
            log.warn("获取新闻失败: {}", errorMsg);
            return null;
        }

        // 校验数据格式
        Object dataObj = newsResult.get("data");
        if (!(dataObj instanceof JSONArray newsArray)) {
            log.warn("新闻数据的data字段不是JSONArray类型");
            return null;
        }

        // 解析新闻列表
        List<Map<String, Object>> newsList = new ArrayList<>();
        for (int i = 0; i < newsArray.size(); i++) {
            try {
                JSONObject jsonObj = newsArray.getJSONObject(i);
                Map<String, Object> newsItem = new HashMap<>();
                newsItem.put("title", jsonObj.getStr(ApiConstants.KEY_NAME, "未知标题"));
                newsItem.put("summary", jsonObj.getStr(BusinessConstants.KEY_SUMMARY, jsonObj.getStr(BusinessConstants.KEY_SNIPPET, "无摘要")));
                newsItem.put("source", jsonObj.getStr(BusinessConstants.KEY_SITE_NAME, "未知来源"));
                newsItem.put("publishTime", jsonObj.getStr(BusinessConstants.KEY_DATE_PUBLISHED, "未知时间"));
                newsItem.put("url", jsonObj.getStr(ApiConstants.KEY_URL, ""));
                newsList.add(newsItem);
            } catch (Exception e) {
                log.warn("解析第{}条新闻数据失败: {}", i + 1, e.getMessage());
            }
        }

        // 生成AI摘要
        Map<String, Object> newsSummaryMap = new HashMap<>();
        newsSummaryMap.put("count", newsList.size());

        if (!newsList.isEmpty()) {
            String summary = aiModelService.generateAnalysis(
                    buildSummarizePrompt(newsList), modelType);
            newsSummaryMap.put("summary", summary);
        } else {
            newsSummaryMap.put("summary", "暂无相关新闻");
        }
        newsSummaryMap.put("newsList", newsList);

        log.info("成功获取{}条新闻", newsList.size());
        return newsSummaryMap;
    }

    private String buildSummarizePrompt(List<Map<String, Object>> newsList) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : newsList) {
            sb.append("- ").append(item.get("title")).append(": ").append(item.get("summary")).append("\n");
        }
        return String.format(BusinessConstants.SUMMARIZE_PROMPT, sb.toString());
    }
}
