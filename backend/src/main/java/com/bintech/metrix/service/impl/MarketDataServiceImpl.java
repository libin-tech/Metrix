package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.dto.request.MarketDataConfigRequest;
import com.bintech.metrix.repository.dao.MarketDataConfigDao;
import com.bintech.metrix.repository.entity.MarketDataConfig;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.MarketDataService;
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

    @Value("${python.executable:python}")
    private String pythonExecutable;

    @Value("${python.tickflow-script-path:python-service/tickflow.py}")
    private String tickflowScriptPath;

    @Value("${python.akshare-script-path:python-service/akshare.py}")
    private String akshareScriptPath;

    private MarketDataConfig getTickFlowConfig(Long userId) {
        List<MarketDataConfig> configs;
        if (userId != null) {
            configs = marketDataConfigDao.selectActiveByUserId(userId);
        } else {
            configs = List.of();
        }
        if (configs.isEmpty()) {
            String errorMsg = "TickFlow市场数据配置不存在";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        MarketDataConfig config = configs.get(0);
        log.debug("成功获取TickFlow配置: id={}", config.getId());
        return config;
    }

    private Map<String, Object> runPythonScript(String scriptName, Long userId, String... scriptArgs) {
        MarketDataConfig config = getTickFlowConfig(userId);
        int timeoutSeconds = config.getTimeout() != null ? config.getTimeout() : SystemConstants.DEFAULT_TIMEOUT_SECONDS;

        String scriptPath = tickflowScriptPath.replace("tickflow.py", "tickflow_" + scriptName + ".py");

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);
        command.add("--api-key");
        command.add(config.getApiKey());
        command.addAll(Arrays.asList(scriptArgs));

        log.info("执行TickFlow脚本: {}", String.join(" ", command));

        return runScript(command, "TickFlow", timeoutSeconds);
    }

    @Override
    @Transactional
    public MarketDataConfig createConfig(MarketDataConfigRequest request) {
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

        List<MarketDataConfig> configs = marketDataConfigDao.selectActiveByUserId(userId);
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
            return !marketDataConfigDao.selectActiveByUserId(userId).isEmpty();
        }
        return false;
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
        return runPythonScript("chip", userId, "--symbols", stockBasic.getTsCode());
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
        String timeoutMsg = sourceName + "脚本执行超时（" + timeoutSeconds + "秒）";

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("PYTHONIOENCODING", "utf-8");

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
    public Map<String, Object> fetchDepthData(StockBasic stockBasic) {
        Long userId = StpUtil.getLoginIdAsLong();
        return fetchDepthData(stockBasic, userId);
    }

    @Override
    public Map<String, Object> fetchDepthData(StockBasic stockBasic, Long userId) {
        log.info("开始查询市场深度（五档行情）: stockCode={}", stockBasic.getTsCode());
        return runPythonScript("depth", userId, "--symbol", stockBasic.getTsCode());
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
