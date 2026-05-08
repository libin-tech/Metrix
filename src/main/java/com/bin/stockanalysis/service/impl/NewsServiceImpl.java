package com.bin.stockanalysis.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bin.stockanalysis.dto.request.NewsSourceConfigRequest;
import com.bin.stockanalysis.repository.entity.NewsSourceConfig;
import com.bin.stockanalysis.repository.mapper.NewsSourceConfigMapper;
import com.bin.stockanalysis.service.AiModelService;
import com.bin.stockanalysis.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsSourceConfigMapper configMapper;
    private final AiModelService aiModelService;

    @Override
    @Transactional
    public NewsSourceConfig createConfig(NewsSourceConfigRequest request) {
        NewsSourceConfig config = new NewsSourceConfig();
        config.setSourceName(request.getSourceName());
        config.setApiUrl(request.getApiUrl());
        config.setApiKey(request.getApiKey());
        config.setRequestInterval(request.getRequestInterval());
        config.setIsActive(request.getIsActive());
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        configMapper.insert(config);
        return config;
    }

    @Override
    @Transactional
    public NewsSourceConfig updateConfig(Long id, NewsSourceConfigRequest request) {
        NewsSourceConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("News source config not found");
        }
        config.setSourceName(request.getSourceName());
        config.setApiUrl(request.getApiUrl());
        config.setApiKey(request.getApiKey());
        config.setRequestInterval(request.getRequestInterval());
        config.setIsActive(request.getIsActive());
        config.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(config);
        return config;
    }

    @Override
    public NewsSourceConfig getConfigById(Long id) {
        NewsSourceConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("News source config not found");
        }
        return config;
    }

    @Override
    public List<NewsSourceConfig> getAllConfigs() {
        return configMapper.selectList(null);
    }

    @Override
    public List<NewsSourceConfig> getActiveConfigs() {
        LambdaQueryWrapper<NewsSourceConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NewsSourceConfig::getIsActive, true);
        return configMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        configMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> fetchStockNews(String stockCode) {
        LambdaQueryWrapper<NewsSourceConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NewsSourceConfig::getSourceName, "BOCHA");
        NewsSourceConfig config = configMapper.selectOne(queryWrapper);
        if (config == null) {
            throw new RuntimeException("Bocha news source config not found");
        }

        Map<String, Object> result = new HashMap<>();
        
        try {
            String url = config.getApiUrl() + "/api/search/news";
            
            HttpResponse response = HttpRequest.get(url)
                    .charset(StandardCharsets.UTF_8)
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .form("keyword", stockCode)
                    .form("limit", 10)
                    .execute();

            String responseBody = response.body();
            JSONObject jsonResult = JSONUtil.parseObj(responseBody);
            
            if ("success".equals(jsonResult.getStr("status"))) {
                result.put("status", "success");
                result.put("data", jsonResult.getJSONArray("data"));
                result.put("count", jsonResult.getInt("count"));
            } else {
                result.put("status", "error");
                result.put("message", jsonResult.getStr("message"));
            }
        } catch (Exception e) {
            log.error("Failed to fetch news from Bocha", e);
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    @Override
    public String summarizeNews(List<Map<String, Object>> newsList) {
        StringBuilder newsText = new StringBuilder();
        for (Map<String, Object> news : newsList) {
            newsText.append("标题: ").append(news.get("title")).append("\n");
            newsText.append("摘要: ").append(news.get("summary")).append("\n");
            newsText.append("来源: ").append(news.get("source")).append("\n\n");
        }

        String prompt = "请对以下股票相关新闻进行总结分析：\n\n" + newsText.toString() + "\n\n请提供简洁的总结，包括主要事件、市场影响和投资建议。";
        
        try {
            return aiModelService.generateAnalysis(prompt, "OPENAI");
        } catch (Exception e) {
            log.error("Failed to summarize news", e);
            return "新闻摘要生成失败: " + e.getMessage();
        }
    }
}
