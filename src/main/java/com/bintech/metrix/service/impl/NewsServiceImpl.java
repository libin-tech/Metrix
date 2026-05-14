package com.bintech.metrix.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bintech.metrix.dto.request.NewsSourceConfigRequest;
import com.bintech.metrix.repository.entity.NewsSourceConfig;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.repository.mapper.NewsSourceConfigMapper;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.NewsService;
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
        if (Boolean.TRUE.equals(request.getIsActive())) {
            configMapper.update(null, new LambdaUpdateWrapper<NewsSourceConfig>()
                    .set(NewsSourceConfig::getIsActive, false));
        }

        NewsSourceConfig config = new NewsSourceConfig();
        config.setSourceName(request.getSourceName());
        config.setApiUrl(request.getApiUrl());
        config.setApiKey(request.getApiKey());
        config.setRequestInterval(request.getRequestInterval());
        config.setIsActive(request.getIsActive());
        config.setTimeout(request.getTimeout());
        config.setRemark(request.getRemark());
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

        if (Boolean.TRUE.equals(request.getIsActive())) {
            configMapper.update(null, new LambdaUpdateWrapper<NewsSourceConfig>()
                    .set(NewsSourceConfig::getIsActive, false)
                    .ne(NewsSourceConfig::getId, id));
        }

        config.setSourceName(request.getSourceName());
        config.setApiUrl(request.getApiUrl());
        config.setApiKey(request.getApiKey());
        config.setRequestInterval(request.getRequestInterval());
        config.setIsActive(request.getIsActive());
        config.setTimeout(request.getTimeout());
        config.setRemark(request.getRemark());
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
    public Map<String, Object> fetchStockNews(StockBasic stockBasic) {
        log.info("开始获取股票新闻: stockCode={}", stockBasic.getTsCode());
        
        LambdaQueryWrapper<NewsSourceConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NewsSourceConfig::getSourceName, "BOCHA");
        NewsSourceConfig config = configMapper.selectOne(queryWrapper);
        if (config == null) {
            String errorMsg = "Bocha新闻源配置不存在";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        Map<String, Object> result = new HashMap<>();
        
        try {
            // 构建请求URL，处理末尾斜杠
            String apiUrl = config.getApiUrl();
            if (apiUrl.endsWith("/")) {
                apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
            }
            String url = apiUrl + "/v1/web-search";
            
            log.info("调用博查搜索API: {}", url);
            
            // 构建请求体（JSON格式）
            JSONObject requestBody = new JSONObject();
            requestBody.set("query",  "搜索 " + stockBasic.getTsCode() + " " +  stockBasic.getName() +"  股票的当前时间近一周内最相关的重要新闻、公告、舆情信息");
            requestBody.set("count", 10);
            requestBody.set("freshness", "oneWeek");
            requestBody.set("summary", true);
            
            int timeoutMs = (config.getTimeout() != null ? config.getTimeout() : 60) * 1000;

            HttpResponse response = HttpRequest.post(url)
                    .charset(StandardCharsets.UTF_8)
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(timeoutMs)
                    .execute();

            int statusCode = response.getStatus();
            log.info("博查API响应状态码: {}", statusCode);
            
            if (statusCode >= 400) {
                result.put("status", "error");
                result.put("message", String.format("请求失败(HTTP %d)", statusCode));
                log.error("博查API请求失败: HTTP {}", statusCode);
                return result;
            }
            
            String responseBody = response.body();
            
            if (responseBody == null || responseBody.isEmpty()) {
                result.put("status", "error");
                result.put("message", "从博查服务器接收到空响应");
                log.error("博查API返回空响应");
                return result;
            }
            
            JSONObject jsonResult = JSONUtil.parseObj(responseBody);
            log.debug("博查API响应: {}", responseBody);
            
            // 解析博查API响应格式
            int code = jsonResult.getInt("code", -1);
            if (code == 200) {
                JSONObject data = jsonResult.getJSONObject("data");
                if (data != null) {
                    JSONObject webPages = data.getJSONObject("webPages");
                    if (webPages != null) {
                        result.put("status", "success");
                        result.put("data", webPages.getJSONArray("value"));
                        result.put("count", webPages.getInt("totalEstimatedMatches", 0));
                        int resultCount = webPages.getJSONArray("value") != null 
                                ? webPages.getJSONArray("value").size() : 0;
                        log.info("博查新闻搜索成功，返回{}条结果", resultCount);
                    } else {
                        result.put("status", "error");
                        result.put("message", "响应数据中缺少webPages字段");
                        log.error("博查API响应缺少webPages字段");
                    }
                } else {
                    result.put("status", "error");
                    result.put("message", "响应数据为空");
                    log.error("博查API响应data字段为空");
                }
            } else {
                String errorMsg = jsonResult.getStr("msg", "未知错误");
                result.put("status", "error");
                result.put("message", String.format("请求失败(代码: %d): %s", code, errorMsg));
                log.error("博查API返回错误: code={}, msg={}", code, errorMsg);
            }
        } catch (Exception e) {
            log.error("获取新闻失败", e);
            result.put("status", "error");
            result.put("message", "获取新闻失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public String summarizeNews(List<Map<String, Object>> newsList, String modelType) {
        StringBuilder newsText = new StringBuilder();
        for (Map<String, Object> news : newsList) {
            newsText.append("标题: ").append(news.get("title")).append("\n");
            newsText.append("摘要: ").append(news.get("summary")).append("\n");
            newsText.append("来源: ").append(news.get("source")).append("\n\n");
        }

        String prompt = "请对以下股票相关新闻进行总结分析：\n\n" + newsText + "\n\n请提供简洁的总结，包括主要事件、市场影响和投资建议。";
        
        try {
            return aiModelService.generateAnalysis(prompt, modelType);
        } catch (Exception e) {
            log.error("Failed to summarize news", e);
            return "新闻摘要生成失败: " + e.getMessage();
        }
    }
}
