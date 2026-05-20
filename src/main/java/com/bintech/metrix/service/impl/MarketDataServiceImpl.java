package com.bintech.metrix.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;


import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.repository.entity.StockBasic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bintech.metrix.dto.request.MarketDataConfigRequest;
import com.bintech.metrix.repository.entity.MarketDataConfig;
import com.bintech.metrix.repository.mapper.MarketDataConfigMapper;
import com.bintech.metrix.service.MarketDataService;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataServiceImpl implements MarketDataService {

    private final MarketDataConfigMapper configMapper;

    @Value("${python.executable:python}")
    private String pythonExecutable;

    @Value("${python.tickflow-script-path:python-service/tickflow.py}")
    private String tickflowScriptPath;

    @Value("${python.akshare-script-path:python-service/akshare.py}")
    private String akshareScriptPath;

    /**
     * 获取TickFlow数据源配置
     */
    private MarketDataConfig getTickFlowConfig() {
        LambdaQueryWrapper<MarketDataConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MarketDataConfig::getIsActive, true);
        MarketDataConfig config = configMapper.selectOne(queryWrapper);

        if (config == null) {
            String errorMsg = "TickFlow市场数据配置不存在";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        log.debug("成功获取TickFlow配置: id={}", config.getId());
        return config;
    }

    /**
     * 执行TickFlow Python脚本获取市场数据
     *
     * @param subcommand 子命令（quotes/depth/klines）
     * @param scriptArgs 脚本参数
     * @return 解析后的JSON结果Map
     */
    private Map<String, Object> runPythonScript(String subcommand, String... scriptArgs) {
        MarketDataConfig config = getTickFlowConfig();
        int timeoutSeconds = config.getTimeout() != null ? config.getTimeout() : SystemConstants.DEFAULT_TIMEOUT_SECONDS;

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(tickflowScriptPath);
        command.add("--api-key");
        command.add(config.getApiKey());
        command.add(subcommand);
        command.addAll(Arrays.asList(scriptArgs));

        log.info("执行TickFlow脚本: {}", String.join(" ", command));

        return runScript(command, "TickFlow", timeoutSeconds);
    }

    @Override
    @Transactional
    public MarketDataConfig createConfig(MarketDataConfigRequest request) {
        log.info("开始创建市场数据配置: sourceName={}", request.getSourceName());

        if (Boolean.TRUE.equals(request.getIsActive())) {
            configMapper.update(null, new LambdaUpdateWrapper<MarketDataConfig>()
                    .set(MarketDataConfig::getIsActive, false));
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
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());

        configMapper.insert(config);
        log.info("市场数据配置创建成功: id={}", config.getId());

        return config;
    }

    @Override
    @Transactional
    public MarketDataConfig updateConfig(Long id, MarketDataConfigRequest request) {
        log.info("开始更新市场数据配置: id={}", id);

        MarketDataConfig config = configMapper.selectById(id);
        if (config == null) {
            String errorMsg = String.format("市场数据配置不存在: id=%d", id);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (Boolean.TRUE.equals(request.getIsActive())) {
            configMapper.update(null, new LambdaUpdateWrapper<MarketDataConfig>()
                    .set(MarketDataConfig::getIsActive, false)
                    .ne(MarketDataConfig::getId, id));
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

        configMapper.updateById(config);
        log.info("市场数据配置更新成功: id={}", id);

        return config;
    }

    @Override
    public MarketDataConfig getConfigById(Long id) {
        log.debug("查询市场数据配置: id={}", id);
        MarketDataConfig config = configMapper.selectById(id);

        if (config == null) {
            log.warn("市场数据配置不存在: id={}", id);
        } else {
            log.debug("查询到市场数据配置: id={}, sourceName={}", id, config.getSourceName());
        }

        return config;
    }

    @Override
    public List<MarketDataConfig> getAllConfigs() {
        log.debug("查询所有市场数据配置");
        List<MarketDataConfig> configs = configMapper.selectList(null);
        log.debug("查询到{}条市场数据配置", configs.size());
        return configs;
    }

    @Override
    public List<MarketDataConfig> getActiveConfigs() {
        log.debug("查询所有启用的市场数据配置");

        LambdaQueryWrapper<MarketDataConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketDataConfig::getIsActive, true);

        List<MarketDataConfig> configs = configMapper.selectList(wrapper);
        log.debug("查询到{}条启用的市场数据配置", configs.size());

        return configs;
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        log.info("开始删除市场数据配置: id={}", id);

        MarketDataConfig config = configMapper.selectById(id);
        if (config == null) {
            log.warn("市场数据配置不存在，无需删除: id={}", id);
            return;
        }

        configMapper.deleteById(id);
        log.info("市场数据配置删除成功: id={}, sourceName={}", id, config.getSourceName());
    }

    @Override
    public Map<String, Object> fetchRealTimeData(StockBasic stockBasic) {
        log.info("开始获取实时行情: stockCode={}", stockBasic.getTsCode());
        return runPythonScript("quotes", "--symbols", stockBasic.getTsCode());
    }

    @Override
    public Map<String, Object> fetchChipData(StockBasic stockBasic) {
        log.info("开始查询筹码分布: stockCode={}", stockBasic.getTsCode());
        return runAkShareScript("chip", "--symbol", stockBasic.getSymbol());
    }

    private Map<String, Object> runAkShareScript(String subcommand, String... scriptArgs) {
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(akshareScriptPath);
        command.add(subcommand);
        command.addAll(Arrays.asList(scriptArgs));

        log.info("执行AKShare脚本: {}", String.join(" ", command));

        int defaultTimeout = SystemConstants.DEFAULT_TIMEOUT_SECONDS;
        return runScript(command, "AKShare", defaultTimeout);
    }

    /**
     * 通用Python脚本执行器，带超时
     * <p>
     * 使用后台线程读取进程输出以防止管道缓冲区死锁（先 waitFor 再 read 时，
     * 如果子进程输出填满管道缓冲区会导致双方互相等待）。
     *
     * @param command        完整命令行
     * @param sourceName     数据源名称（用于日志）
     * @param timeoutSeconds 超时时间（秒）
     * @return 解析后的JSON结果Map
     */
    private Map<String, Object> runScript(List<String> command, String sourceName, int timeoutSeconds) {
        String timeoutMsg = sourceName + "脚本执行超时（" + timeoutSeconds + "秒）";

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 在后台线程中持续读取输出，避免管道缓冲区写满导致子进程阻塞
            StringBuilder outputBuilder = new StringBuilder();
            Thread reader = Thread.ofVirtual()
                    .name("market-data-reader")
                    .start(() -> {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                outputBuilder.append(line).append('\n');
                            }
                        } catch (IOException e) {
                            log.warn("读取{}脚本输出流异常: {}", sourceName, e.getMessage());
                        }
                    });

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            reader.join(SystemConstants.READER_JOIN_TIMEOUT_MILLIS);

            if (!finished) {
                process.destroyForcibly();
                log.error("{}", timeoutMsg);
                throw new RuntimeException(timeoutMsg);
            }

            String output = outputBuilder.toString().trim();
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

    @Override
    public Map<String, Object> fetchDepthData(StockBasic stockBasic) {
        log.info("开始查询市场深度（五档行情）: stockCode={}", stockBasic.getTsCode());
        return runPythonScript("depth", "--symbol", stockBasic.getTsCode());
    }

    @Override
    public Map<String, Object> fetchKlinesData(StockBasic stockBasic, int limit) {
        log.info("开始查询K线数据: stockCode={}, limit={}", stockBasic.getTsCode(), limit);
        return runPythonScript("klines", "--symbol", stockBasic.getTsCode(), "--count", String.valueOf(limit), "--period", SystemConstants.KLINE_PERIOD_DAY);
    }
}
