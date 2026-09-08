package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.FinancialDataConstants;
import com.bintech.metrix.config.FinancialDataProperties;
import com.bintech.metrix.constants.CacheConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.dto.request.MarketDataConfigRequest;
import com.bintech.metrix.repository.dao.MarketDataConfigDao;
import com.bintech.metrix.repository.entity.MarketDataConfig;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataServiceImpl implements MarketDataService {

    private final MarketDataConfigDao marketDataConfigDao;
    private final RedisCacheService redisCacheService;

    @Value("${python.executable:python}")
    private String pythonExecutable;

    private final FinancialDataProperties financialDataProperties;

    @Value("${python.akshare-script-path:python-service/akshare.py}")
    private String akshareScriptPath;

    private MarketDataConfig getFinancialDataConfig(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("请登录并配置同花顺金融数据 API");
        }
        MarketDataConfig config = marketDataConfigDao.selectActiveByUserId(userId).stream()
                .filter(item -> FinancialDataConstants.SOURCE_NAME.equals(item.getSourceName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("请新增并启用同花顺金融数据 API 配置"));
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("请配置同花顺金融数据 API Key");
        }
        log.debug("成功获取同花顺金融数据 API配置: id={}", config.getId());
        return config;
    }

    private Map<String, Object> runPythonScript(String scriptName, Long userId, String... scriptArgs) {
        MarketDataConfig config = getFinancialDataConfig(userId);
        int timeoutSeconds = config.getTimeout() != null ? config.getTimeout() : SystemConstants.DEFAULT_TIMEOUT_SECONDS;

        String scriptPath = financialDataProperties.getScriptPath();

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);
        command.add("--operation");
        command.add(scriptName);
        command.add("--api-url");
        command.add(config.getApiUrl());
        command.addAll(Arrays.asList(scriptArgs));

        log.info("执行同花顺金融数据 API脚本: {}", scriptPath);

        return runScript(command, "同花顺金融数据 API", timeoutSeconds,
                Map.of(FinancialDataConstants.API_KEY_ENV, config.getApiKey()));
    }

    /** 防止误用旧行情源的鉴权配置。 */
    private void validateFinancialConfig(MarketDataConfigRequest request) {
        if (!FinancialDataConstants.SOURCE_NAME.equals(request.getSourceName())) {
            throw new IllegalArgumentException("行情数据源请使用同花顺金融数据 API（FUYAO）");
        }
        if (request.getApiKey() == null || request.getApiKey().isBlank()) {
            throw new IllegalArgumentException("请填写同花顺金融数据 API Key");
        }
    }

    @Override
    @Transactional
    public MarketDataConfig createConfig(MarketDataConfigRequest request) {
        validateFinancialConfig(request);
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("开始创建市场数据配置: sourceName={}, userId={}", request.getSourceName(), userId);

        if (Boolean.TRUE.equals(request.getIsActive())) {
            marketDataConfigDao.deactivateByUserId(userId);
        }

        MarketDataConfig config = new MarketDataConfig();
        config.setSourceName(request.getSourceName());
        config.setApiUrl(request.getApiUrl());
        config.setApiKey(request.getApiKey());
        config.setDataType(request.getDataType());
        config.setRequestInterval(request.getRequestInterval());
        config.setIsActive(request.getIsActive());
        config.setTimeout(request.getTimeout());
        config.setRemark(request.getRemark());
        config.setUserId(userId);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());

        marketDataConfigDao.insert(config);
        log.info("市场数据配置创建成功: id={}", config.getId());

        return config;
    }

    @Override
    @Transactional
    public MarketDataConfig updateConfig(Long id, MarketDataConfigRequest request) {
        validateFinancialConfig(request);
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("开始更新市场数据配置: id={}", id);

        MarketDataConfig config = marketDataConfigDao.selectByIdAndUserId(id, userId);
        if (config == null) {
            throw new RuntimeException("市场数据配置不存在");
        }

        if (Boolean.TRUE.equals(request.getIsActive())) {
            marketDataConfigDao.deactivateByUserIdAndExcludeId(userId, id);
        }

        config.setSourceName(request.getSourceName());
        config.setApiUrl(request.getApiUrl());
        config.setApiKey(request.getApiKey());
        config.setDataType(request.getDataType());
        config.setRequestInterval(request.getRequestInterval());
        config.setIsActive(request.getIsActive());
        config.setTimeout(request.getTimeout());
        config.setRemark(request.getRemark());
        config.setUpdateTime(LocalDateTime.now());

        marketDataConfigDao.updateById(config);
        log.info("市场数据配置更新成功: id={}", id);

        return config;
    }

    @Override
    public MarketDataConfig getConfigById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("查询市场数据配置: id={}", id);
        MarketDataConfig config = marketDataConfigDao.selectByIdAndUserId(id, userId);

        if (config == null) {
            log.warn("市场数据配置不存在: id={}", id);
        }

        return config;
    }

    @Override
    public List<MarketDataConfig> getAllConfigs() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("查询所有市场数据配置, userId={}", userId);
        List<MarketDataConfig> configs = marketDataConfigDao.selectByUserId(userId);
        log.debug("查询到{}条市场数据配置", configs.size());
        return configs;
    }

    @Override
    public List<MarketDataConfig> getActiveConfigs() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("查询所有启用的市场数据配置, userId={}", userId);

        List<MarketDataConfig> configs = marketDataConfigDao.selectActiveByUserId(userId).stream()
                    .filter(config -> FinancialDataConstants.SOURCE_NAME.equals(config.getSourceName())).toList();
        log.debug("查询到{}条启用的市场数据配置", configs.size());

        return configs;
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("开始删除市场数据配置: id={}", id);

        MarketDataConfig config = marketDataConfigDao.selectByIdAndUserId(id, userId);
        if (config == null) {
            log.warn("市场数据配置不存在，无需删除: id={}", id);
            return;
        }

        marketDataConfigDao.deleteById(id);
        log.info("市场数据配置删除成功: id={}, sourceName={}", id, config.getSourceName());
    }

    @Override
    public boolean hasActiveConfig(Long userId) {
        if (userId != null) {
            return marketDataConfigDao.selectActiveByUserId(userId).stream()
                    .anyMatch(config -> FinancialDataConstants.SOURCE_NAME.equals(config.getSourceName())
                            && config.getApiKey() != null && !config.getApiKey().isBlank());
        }
        return false;
    }

    /** 从已启用的同花顺金融数据源读取完整 A 股标的列表。 */
    @Override
    public Map<String, Object> fetchTickerData(Long userId) {
        return runPythonScript("tickers", userId);
    }

    @Override
    public Map<String, Object> fetchAnomalyAnalysisData(Long userId) {
        return runPythonScript("anomaly-analysis", userId);
    }

    @Override
    public Map<String, Object> fetchMarketTurnoverData(Long userId) {
        return runPythonScript("market-turnover", userId, "--symbols",
                SystemConstants.MARKET_TURNOVER_SHANGHAI_SYMBOL + "," + SystemConstants.MARKET_TURNOVER_SHENZHEN_SYMBOL);
    }

    @Override
    public Map<String, Object> fetchRealTimeData(StockBasic stockBasic) {
        Long userId = StpUtil.getLoginIdAsLong();
        return fetchRealTimeData(stockBasic, userId);
    }

    @Override
    public Map<String, Object> fetchRealTimeData(StockBasic stockBasic, Long userId) {
        log.info("开始获取实时行情: stockCode={}", stockBasic.getTsCode());
        return runPythonScript("quotes", userId, "--symbols", stockBasic.getTsCode());
    }

    @Override
    public Map<String, Object> fetchChipData(StockBasic stockBasic) {
        Long userId = StpUtil.getLoginIdAsLong();
        return fetchChipData(stockBasic, userId);
    }

    @Override
    public Map<String, Object> fetchChipData(StockBasic stockBasic, Long userId) {
        log.info("开始查询筹码分布: stockCode={}", stockBasic.getTsCode());
        String cacheKey = CacheConstants.CHIP_LAST_SUCCESS_KEY_PREFIX + stockBasic.getTsCode();
        try {
            Map<String, Object> result = runAkShareScript("chip", "--symbol", stockBasic.getSymbol());
            cacheChip(cacheKey, result);
            return result;
        } catch (RuntimeException error) {
            log.warn("筹码上游不可用，尝试最近成功缓存: stockCode={}, reason={}", stockBasic.getTsCode(), error.getMessage());
            return readCachedChip(cacheKey);
        }
    }

    /** 缓存写入失败不影响本次成功数据；按完整股票代码隔离前复权筹码。 */
    private void cacheChip(String key, Map<String, Object> result) {
        try {
            redisCacheService.setJson(key, result);
        } catch (RuntimeException error) {
            log.warn("保存筹码缓存失败: {}", error.getMessage());
        }
    }

    /** 保留缓存原交易日期，并明确标记上游失败；无缓存不伪造指标。 */
    private Map<String, Object> readCachedChip(String key) {
        try {
            JSONObject cached = JSONUtil.parseObj(redisCacheService.get(key));
            JSONObject data = cached.getJSONObject(ApiConstants.KEY_DATA);
            if (ApiConstants.STATUS_SUCCESS.equals(cached.getStr(ApiConstants.KEY_STATUS))
                    && data != null && data.getStr("date") != null) {
                data.set("stale", true);
                cached.set(ApiConstants.KEY_MESSAGE, "筹码上游不可用，使用缓存，数据日期：" + data.getStr("date"));
                return cached;
            }
        } catch (RuntimeException error) {
            log.warn("读取筹码缓存失败: {}", error.getMessage());
        }
        return Map.of(ApiConstants.KEY_STATUS, ApiConstants.STATUS_ERROR, ApiConstants.KEY_MESSAGE,
                "筹码上游暂不可用且无成功缓存，本次分析不使用筹码指标");
    }

    private Map<String, Object> runAkShareScript(String scriptName, String... scriptArgs) {
        String scriptPath = akshareScriptPath.replace("akshare.py", "akshare_" + scriptName + ".py");

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);
        command.addAll(Arrays.asList(scriptArgs));

        log.info("执行AKShare脚本: {}", String.join(" ", command));

        int defaultTimeout = SystemConstants.DEFAULT_TIMEOUT_SECONDS;
        return runScript(command, "AKShare", defaultTimeout);
    }

    private Map<String, Object> runScript(List<String> command, String sourceName, int timeoutSeconds) {
        return runScript(command, sourceName, timeoutSeconds, Map.of());
    }

    private Map<String, Object> runScript(List<String> command, String sourceName, int timeoutSeconds,
                                          Map<String, String> environment) {
        String timeoutMsg = sourceName + "脚本执行超时（" + timeoutSeconds + "秒）";

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.environment().putAll(environment);

            Process process = pb.start();

            StringBuilder standardOutput = new StringBuilder();
            StringBuilder standardError = new StringBuilder();
            Thread outputReader = startProcessReader(process.getInputStream(), standardOutput, sourceName, "标准输出");
            Thread errorReader = startProcessReader(process.getErrorStream(), standardError, sourceName, "标准错误");

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                process.waitFor();
            }

            outputReader.join(SystemConstants.READER_JOIN_TIMEOUT_MILLIS);
            errorReader.join(SystemConstants.READER_JOIN_TIMEOUT_MILLIS);

            if (!finished) {
                log.error("{}", timeoutMsg);
                throw new RuntimeException(timeoutMsg);
            }

            String output = standardOutput.toString().trim();
            String errorOutput = standardError.toString().trim();
            if (!errorOutput.isEmpty()) {
                log.warn("{}脚本标准错误输出: {}", sourceName, errorOutput);
            }
            if (output.isEmpty()) {
                log.error("{}脚本输出为空", sourceName);
                throw new RuntimeException(sourceName + "数据获取失败: 脚本输出为空");
            }

            JSONObject json = JSONUtil.parseObj(output);

            if (!ApiConstants.STATUS_SUCCESS.equals(json.getStr(ApiConstants.KEY_STATUS))) {
                String msg = json.getStr(ApiConstants.KEY_MESSAGE, sourceName + "脚本执行失败");
                log.error("{}脚本返回错误: {}", sourceName, msg);
                throw new RuntimeException(sourceName + "数据获取失败: " + msg);
            }

            log.info("{}脚本返回结果: {}", sourceName, json);
            return json;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行{}脚本异常: {}", sourceName, e.getMessage(), e);
            throw new RuntimeException(sourceName + "数据获取异常: " + e.getMessage());
        }
    }

    private Thread startProcessReader(InputStream inputStream, StringBuilder output,
                                      String sourceName, String streamName) {
        return Thread.ofVirtual()
                .name("market-data-" + streamName + "-reader")
                .start(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append('\n');
                        }
                    } catch (IOException e) {
                        log.warn("读取{}脚本{}异常: {}", sourceName, streamName, e.getMessage());
                    }
                });
    }

    @Override
    public Map<String, Object> fetchKlinesData(StockBasic stockBasic, int limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        return fetchKlinesData(stockBasic, limit, userId);
    }

    @Override
    public Map<String, Object> fetchKlinesData(StockBasic stockBasic, int limit, Long userId) {
        log.info("开始查询K线数据: stockCode={}, limit={}", stockBasic.getTsCode(), limit);
        return runPythonScript("klines", userId, "--symbol", stockBasic.getTsCode(), "--count", String.valueOf(limit), "--period", SystemConstants.KLINE_PERIOD_DAY);
    }

    @Override
    public Map<String, Object> fetchTopFreeShareholdersData(StockBasic stockBasic) {
        Long userId = StpUtil.getLoginIdAsLong();
        return fetchTopFreeShareholdersData(stockBasic, userId);
    }

    @Override
    public Map<String, Object> fetchTopFreeShareholdersData(StockBasic stockBasic, Long userId) {
        log.info("开始查询十大流通股东: stockCode={}", stockBasic.getTsCode());
        return runAkShareScript("gdfx", "--symbol", stockBasic.getSymbol());
    }
}
