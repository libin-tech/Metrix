package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.dto.request.NewsSourceConfigRequest;
import com.bintech.metrix.repository.dao.NewsSourceConfigDao;
import com.bintech.metrix.repository.entity.NewsSourceConfig;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsSourceConfigDao newsSourceConfigDao;
    private final AiModelService aiModelService;

    @Value("${python.executable:python}")
    private String pythonExecutable;

    @Value("${python.akshare-script-path:python-service/akshare.py}")
    private String akshareScriptPath;

    @Override
    @Transactional
    public NewsSourceConfig createConfig(NewsSourceConfigRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (Boolean.TRUE.equals(request.getIsActive())) {
            newsSourceConfigDao.deactivateByUserId(userId);
        }

        NewsSourceConfig config = new NewsSourceConfig();
        config.setSourceName(request.getSourceName());
        config.setApiUrl(request.getApiUrl());
        config.setApiKey(request.getApiKey());
        config.setRequestInterval(request.getRequestInterval());
        config.setIsActive(request.getIsActive());
        config.setTimeout(request.getTimeout());
        config.setRemark(request.getRemark());
        config.setUserId(userId);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        newsSourceConfigDao.insert(config);
        return config;
    }

    @Override
    @Transactional
    public NewsSourceConfig updateConfig(Long id, NewsSourceConfigRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        NewsSourceConfig config = newsSourceConfigDao.selectByIdAndUserId(id, userId);
        if (config == null) {
            throw new RuntimeException("News source config not found");
        }

        if (Boolean.TRUE.equals(request.getIsActive())) {
            newsSourceConfigDao.deactivateByUserIdAndExcludeId(userId, id);
        }

        config.setSourceName(request.getSourceName());
        config.setApiUrl(request.getApiUrl());
        config.setApiKey(request.getApiKey());
        config.setRequestInterval(request.getRequestInterval());
        config.setIsActive(request.getIsActive());
        config.setTimeout(request.getTimeout());
        config.setRemark(request.getRemark());
        config.setUpdateTime(LocalDateTime.now());
        newsSourceConfigDao.updateById(config);
        return config;
    }

    @Override
    public NewsSourceConfig getConfigById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        NewsSourceConfig config = newsSourceConfigDao.selectByIdAndUserId(id, userId);
        if (config == null) {
            throw new RuntimeException("News source config not found");
        }
        return config;
    }

    @Override
    public List<NewsSourceConfig> getAllConfigs() {
        Long userId = StpUtil.getLoginIdAsLong();
        return newsSourceConfigDao.selectByUserId(userId);
    }

    @Override
    public List<NewsSourceConfig> getActiveConfigs() {
        Long userId = StpUtil.getLoginIdAsLong();
        return newsSourceConfigDao.selectActiveByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        long count = newsSourceConfigDao.countByIdAndUserId(id, userId);
        if (count == 0) {
            throw new RuntimeException("News source config not found");
        }
        newsSourceConfigDao.deleteById(id);
    }

    @Override
    public boolean hasActiveNewsSource(Long userId) {
        if (userId != null) {
            return newsSourceConfigDao.countActiveByUserId(userId) > 0;
        }
        return false;
    }

    @Override
    public Map<String, Object> fetchStockNews(StockBasic stockBasic) {
        Long userId = StpUtil.getLoginIdAsLong();
        return fetchStockNews(stockBasic, userId);
    }

    @Override
    public Map<String, Object> fetchStockNews(StockBasic stockBasic, Long userId) {
        log.info("开始获取股票新闻: stockCode={}", stockBasic.getTsCode());

        Map<String, Object> result = tryAkShareNews(stockBasic);
        if (result != null) {
            return result;
        }

        log.info("AKShare新闻获取失败，使用Bocha兜底: stockCode={}", stockBasic.getTsCode());
        return fetchBochaNews(stockBasic, userId);
    }

    private Map<String, Object> tryAkShareNews(StockBasic stockBasic) {
        try {
            String scriptPath = akshareScriptPath.replace("akshare.py", "akshare_news.py");

            List<String> command = new ArrayList<>();
            command.add(pythonExecutable);
            command.add(scriptPath);
            command.add("--symbol");
            command.add(stockBasic.getSymbol());

            log.info("执行AKShare新闻脚本: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            Process process = pb.start();

            StringBuilder outputBuilder = new StringBuilder();
            Thread reader = Thread.ofVirtual()
                    .name("akshare-news-reader")
                    .start(() -> {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                outputBuilder.append(line).append('\n');
                            }
                        } catch (Exception e) {
                            log.warn("读取AKShare新闻脚本输出流异常: {}", e.getMessage());
                        }
                    });

            boolean finished = process.waitFor(SystemConstants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            reader.join(SystemConstants.READER_JOIN_TIMEOUT_MILLIS);

            if (!finished) {
                process.destroyForcibly();
                log.warn("AKShare新闻脚本执行超时");
                return null;
            }

            String output = outputBuilder.toString().trim();
            if (output.isEmpty()) {
                log.warn("AKShare新闻脚本输出为空");
                return null;
            }

            log.debug("AKShare新闻脚本原始输出: {}", output);

            JSONObject json = JSONUtil.parseObj(output);
            if (!ApiConstants.STATUS_SUCCESS.equals(json.getStr(ApiConstants.KEY_STATUS))) {
                log.warn("AKShare新闻获取失败: {}", json.getStr(ApiConstants.KEY_MESSAGE));
                return null;
            }

            JSONArray data = json.getJSONArray(ApiConstants.KEY_DATA);
            Map<String, Object> result = new HashMap<>();
            result.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_SUCCESS);
            result.put(ApiConstants.KEY_DATA, data != null ? data : new JSONArray());
            result.put(ApiConstants.KEY_COUNT, json.getInt(ApiConstants.KEY_COUNT, 0));
            log.info("AKShare新闻获取成功，共{}条", json.getInt(ApiConstants.KEY_COUNT, 0));
            return result;
        } catch (Exception e) {
            log.warn("AKShare新闻脚本执行异常: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> fetchBochaNews(StockBasic stockBasic, Long userId) {
        List<NewsSourceConfig> configs;
        if (userId != null) {
            configs = newsSourceConfigDao.selectByUserId(userId);
        } else {
            configs = List.of();
        }
        NewsSourceConfig config = configs.stream()
                .filter(c -> BusinessConstants.SOURCE_NAME_BOCHA.equals(c.getSourceName()))
                .findFirst()
                .orElse(null);
        if (config == null) {
            String errorMsg = "Bocha新闻源配置不存在";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        Map<String, Object> result = new HashMap<>();

        try {
            String apiUrl = config.getApiUrl();
            if (apiUrl.endsWith(SystemConstants.URL_TRAILING_SLASH)) {
                apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
            }
            String url = apiUrl + BusinessConstants.BOCHA_API_PATH;

            log.info("调用博查搜索API: {}", url);

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

            if (statusCode >= HttpStatus.BAD_REQUEST.value()) {
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
            if (code == HttpStatus.OK.value()) {
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
        JSONArray rawPages = webPages.getJSONArray(ApiConstants.KEY_VALUE);
        JSONArray normalizedPages = new JSONArray();
        if (rawPages != null) {
            for (int i = 0; i < rawPages.size(); i++) {
                JSONObject raw = rawPages.getJSONObject(i);
                JSONObject item = new JSONObject();
                item.set(ApiConstants.KEY_TITLE, raw.getStr("name", ""));
                item.set(BusinessConstants.KEY_SUMMARY, raw.getStr(BusinessConstants.KEY_SUMMARY, raw.getStr(BusinessConstants.KEY_SNIPPET, "")));
                item.set(ApiConstants.KEY_SOURCE, raw.getStr(BusinessConstants.KEY_SITE_NAME, ""));
                item.set("publishTime", raw.getStr(BusinessConstants.KEY_DATE_PUBLISHED, ""));
                item.set(ApiConstants.KEY_URL, raw.getStr(ApiConstants.KEY_URL, ""));
                normalizedPages.add(item);
            }
        }
        result.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_SUCCESS);
        result.put(ApiConstants.KEY_DATA, normalizedPages);
        result.put(ApiConstants.KEY_COUNT, webPages.getInt(BusinessConstants.KEY_TOTAL_ESTIMATED_MATCHES, 0));
        int resultCount = normalizedPages.size();
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
