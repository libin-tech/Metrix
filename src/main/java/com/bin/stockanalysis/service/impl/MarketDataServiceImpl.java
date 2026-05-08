package com.bin.stockanalysis.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bin.stockanalysis.dto.request.MarketDataConfigRequest;
import com.bin.stockanalysis.repository.entity.MarketDataConfig;
import com.bin.stockanalysis.repository.mapper.MarketDataConfigMapper;
import com.bin.stockanalysis.service.MarketDataService;
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
public class MarketDataServiceImpl implements MarketDataService {

    private final MarketDataConfigMapper configMapper;

    @Override
    @Transactional
    public MarketDataConfig createConfig(MarketDataConfigRequest request) {
        MarketDataConfig config = new MarketDataConfig();
        config.setSourceName(request.getSourceName());
        config.setApiUrl(request.getApiUrl());
        config.setApiKey(request.getApiKey());
        config.setDataType(request.getDataType());
        config.setRequestInterval(request.getRequestInterval());
        config.setIsActive(request.getIsActive());
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        configMapper.insert(config);
        return config;
    }

    @Override
    @Transactional
    public MarketDataConfig updateConfig(Long id, MarketDataConfigRequest request) {
        MarketDataConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("Market data config not found");
        }
        config.setSourceName(request.getSourceName());
        config.setApiUrl(request.getApiUrl());
        config.setApiKey(request.getApiKey());
        config.setDataType(request.getDataType());
        config.setRequestInterval(request.getRequestInterval());
        config.setIsActive(request.getIsActive());
        config.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(config);
        return config;
    }

    @Override
    public MarketDataConfig getConfigById(Long id) {
        MarketDataConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("Market data config not found");
        }
        return config;
    }

    @Override
    public List<MarketDataConfig> getAllConfigs() {
        return configMapper.selectList(null);
    }

    @Override
    public List<MarketDataConfig> getActiveConfigs() {
        LambdaQueryWrapper<MarketDataConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MarketDataConfig::getIsActive, true);
        return configMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        configMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> fetchMarketData(String stockCode) {
        LambdaQueryWrapper<MarketDataConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MarketDataConfig::getSourceName, "TICKFLOW");
        MarketDataConfig config = configMapper.selectOne(queryWrapper);
        if (config == null) {
            throw new RuntimeException("TickFlow market data config not found");
        }

        Map<String, Object> result = new HashMap<>();
        
        try {
            String url = config.getApiUrl() + "/api/stock/quote";
            
            HttpResponse response = HttpRequest.get(url)
                    .charset(StandardCharsets.UTF_8)
                    .header("X-API-Key", config.getApiKey())
                    .form("symbol", stockCode)
                    .form("fields", "open,high,low,close,volume,change,changePercent")
                    .execute();

            String responseBody = response.body();
            JSONObject jsonResult = JSONUtil.parseObj(responseBody);
            
            if ("success".equals(jsonResult.getStr("status"))) {
                result.put("status", "success");
                result.put("data", jsonResult.getJSONObject("data"));
            } else {
                result.put("status", "error");
                result.put("message", jsonResult.getStr("message"));
            }
        } catch (Exception e) {
            log.error("Failed to fetch market data from TickFlow", e);
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> fetchRealTimeData(String stockCode) {
        return fetchMarketData(stockCode);
    }
}
