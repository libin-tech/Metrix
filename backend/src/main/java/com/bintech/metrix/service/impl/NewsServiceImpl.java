package com.bintech.metrix.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.constants.SystemConstants;
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
        queryWrapper.eq(NewsSourceConfig::getSourceName, BusinessConstants.SOURCE_NAME_BOCHA);
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
            if (apiUrl.endsWith(SystemConstants.URL_TRAILING_SLASH)) {
                apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
            }
            String url = apiUrl + BusinessConstants.BOCHA_API_PATH;
            
            log.info("调用博查搜索API: {}", url);
            
            // 构建请求体（JSON格式）
            JSONObject requestBody = new JSONObject();
            requestBody.set(ApiConstants.KEY_QUERY, String.format(BusinessConstants.BOCHA_SEARCH_QUERY, stockBasic.getTsCode(), stockBasic.getName()));
            requestBody.set(ApiConstants.KEY_COUNT, BusinessConstants.DEFAULT_NEWS_COUNT);
            requestBody.set("freshness", BusinessConstants.NEWS_FRESHNESS);
            requestBody.set(BusinessConstants.KEY_SUMMARY, true);
            
            int timeoutMs = (config.getTimeout() != null ? config.getTimeout() : SystemConstants.DEFAULT_TIMEOUT_SECONDS) * SystemConstants.MILLIS_PER_SECOND;

            HttpResponse response = HttpRequest.post(url)
                    .charset(StandardCharsets.UTF_8)
                    .header(ApiConstants.HEADER_AUTHORIZATION, ApiConstants.AUTH_BEARER_PREFIX + config.getApiKey())
                    .header(ApiConstants.HEADER_CONTENT_TYPE, ApiConstants.CONTENT_TYPE_JSON)
                    .body(requestBody.toString())
                    .timeout(timeoutMs)
                    .execute();

            int statusCode = response.getStatus();
            log.info("博查API响应状态码: {}", statusCode);
            
            if (statusCode >= ApiConstants.HTTP_STATUS_BAD_REQUEST) {
                result.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_ERROR);
                result.put(ApiConstants.KEY_MESSAGE, String.format("请求失败(HTTP %d)", statusCode));
                log.error("博查API请求失败: HTTP {}", statusCode);
                return result;
            }

            String responseBody = response.body();

            if (responseBody == null || responseBody.isEmpty()) {
                result.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_ERROR);
                result.put(ApiConstants.KEY_MESSAGE, "从博查服务器接收到空响应");
                log.error("博查API返回空响应");
                return result;
            }

            JSONObject jsonResult = JSONUtil.parseObj(responseBody);
            log.debug("博查API响应: {}", responseBody);

            int code = jsonResult.getInt(ApiConstants.KEY_CODE, -1);
            if (code == ApiConstants.HTTP_STATUS_OK) {
                parseBochaResult(jsonResult, result);
            } else {
                result.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_ERROR);
                result.put(ApiConstants.KEY_MESSAGE, "博查API返回错误码: " + code);
                log.warn("博查新闻搜索API返回非200状态码: {}", code);
            }
        } catch (Exception e) {
            log.error("搜索新闻失败: stockCode={}", stockBasic.getTsCode(), e);
            result.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_ERROR);
            result.put(ApiConstants.KEY_MESSAGE, "新闻接口调用失败");
        }
        return result;
    }

    private void parseBochaResult(JSONObject jsonResult, Map<String, Object> result) {
        JSONObject data = jsonResult.getJSONObject(ApiConstants.KEY_DATA);
        if (data == null) {
            result.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_SUCCESS);
            result.put(ApiConstants.KEY_DATA, new JSONArray());
            result.put(ApiConstants.KEY_COUNT, 0);
            log.info("博查新闻搜索成功，但无数据返回");
            return;
        }
        JSONObject webPages = data.getJSONObject(BusinessConstants.KEY_WEB_PAGES);
        if (webPages == null) {
            result.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_ERROR);
            result.put(ApiConstants.KEY_MESSAGE, "响应数据中缺少webPages字段");
            log.error("博查API响应缺少webPages字段");
            return;
        }
        result.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_SUCCESS);
        result.put(ApiConstants.KEY_DATA, webPages.getJSONArray(ApiConstants.KEY_VALUE));
        result.put(ApiConstants.KEY_COUNT, webPages.getInt(BusinessConstants.KEY_TOTAL_ESTIMATED_MATCHES, 0));
        int resultCount = webPages.getJSONArray(ApiConstants.KEY_VALUE) != null 
                ? webPages.getJSONArray(ApiConstants.KEY_VALUE).size() : 0;
        log.info("博查新闻搜索成功，返回{}条结果", resultCount);
    }

    @Override
    public String summarizeNews(List<Map<String, Object>> newsList, String modelType) {
        StringBuilder newsText = new StringBuilder();
        for (Map<String, Object> news : newsList) {
            newsText.append("标题: ").append(news.get(ApiConstants.KEY_TITLE)).append("\n");
            newsText.append("摘要: ").append(news.get(BusinessConstants.KEY_SUMMARY)).append("\n");
            newsText.append("来源: ").append(news.get(ApiConstants.KEY_SOURCE)).append("\n\n");
        }

        String prompt = String.format(BusinessConstants.SUMMARIZE_PROMPT, newsText);
        
        try {
            return aiModelService.generateAnalysis(prompt, modelType);
        } catch (Exception e) {
            log.error("Failed to summarize news", e);
            return "新闻摘要生成失败: " + e.getMessage();
        }
    }
}
