package com.bintech.metrix.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.service.MarketInsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 通过 AKShare 获取首页市场洞察数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketInsightServiceImpl implements MarketInsightService {

    private static final String AKSHARE_SCRIPT_NAME = "akshare.py";
    private static final String MARKET_INSIGHTS_SCRIPT_NAME = "akshare_market_insights.py";
    private static final String SOURCE_NAME = "MarketInsights";

    @Value("${python.executable:python}")
    private String pythonExecutable;

    @Value("${python.akshare-script-path:python-service/akshare.py}")
    private String akshareScriptPath;

    /**
     * 获取首页展示的龙虎榜和资金流向数据。
     */
    @Override
    public Map<String, Object> getMarketInsights() {
        String scriptPath = akshareScriptPath.replace(AKSHARE_SCRIPT_NAME, MARKET_INSIGHTS_SCRIPT_NAME);
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);

        log.info("执行首页市场洞察脚本: {}", String.join(" ", command));
        return runScript(command);
    }

    private Map<String, Object> runScript(List<String> command) {
        String timeoutMessage = SOURCE_NAME + "脚本执行超时（" + SystemConstants.DEFAULT_TIMEOUT_SECONDS + "秒）";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
            Process process = processBuilder.start();

            StringBuilder outputBuilder = new StringBuilder();
            Thread reader = Thread.ofVirtual()
                    .name("market-insights-reader")
                    .start(() -> readProcessOutput(process, outputBuilder));

            boolean finished = process.waitFor(SystemConstants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            reader.join(SystemConstants.READER_JOIN_TIMEOUT_MILLIS);
            if (!finished) {
                process.destroyForcibly();
                log.error("{}", timeoutMessage);
                throw new RuntimeException(timeoutMessage);
            }

            String output = outputBuilder.toString().trim();
            if (output.isEmpty()) {
                throw new RuntimeException(SOURCE_NAME + "数据获取失败: 脚本输出为空");
            }

            JSONObject response = JSONUtil.parseObj(output);
            if (!ApiConstants.STATUS_SUCCESS.equals(response.getStr(ApiConstants.KEY_STATUS))) {
                String message = response.getStr(ApiConstants.KEY_MESSAGE, SOURCE_NAME + "脚本执行失败");
                throw new RuntimeException(SOURCE_NAME + "数据获取失败: " + message);
            }
            return response;
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("执行{}脚本异常: {}", SOURCE_NAME, exception.getMessage(), exception);
            throw new RuntimeException(SOURCE_NAME + "数据获取异常: " + exception.getMessage());
        }
    }

    private void readProcessOutput(Process process, StringBuilder outputBuilder) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputBuilder.append(line).append('\n');
            }
        } catch (IOException exception) {
            log.warn("读取{}脚本输出流异常: {}", SOURCE_NAME, exception.getMessage());
        }
    }
}
