package com.bintech.metrix.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.service.MarketActivityService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketActivityServiceImpl implements MarketActivityService {

    @Value("${python.executable:python}")
    private String pythonExecutable;

    @Value("${python.akshare-script-path:python-service/akshare.py}")
    private String akshareScriptPath;

    @Override
    public Map<String, Object> getMarketActivity() {
        String scriptPath = akshareScriptPath.replace("akshare.py", "akshare_market_activity.py");

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);

        log.info("执行赚钱效应分析脚本: {}", String.join(" ", command));

        return runScript(command, "MarketActivity", SystemConstants.DEFAULT_TIMEOUT_SECONDS);
    }

    private Map<String, Object> runScript(List<String> command, String sourceName, int timeoutSeconds) {
        String timeoutMsg = sourceName + "脚本执行超时（" + timeoutSeconds + "秒）";

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            Process process = pb.start();

            StringBuilder outputBuilder = new StringBuilder();
            Thread reader = Thread.ofVirtual()
                    .name("market-activity-reader")
                    .start(() -> {
                        try (BufferedReader br = new BufferedReader(
                                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
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

            log.info("{}脚本输出: {}", sourceName, output);

            JSONObject json = JSONUtil.parseObj(output);

            if (!ApiConstants.STATUS_SUCCESS.equals(json.getStr(ApiConstants.KEY_STATUS))) {
                String msg = json.getStr(ApiConstants.KEY_MESSAGE, sourceName + "脚本执行失败");
                log.error("{}脚本返回错误: {}", sourceName, msg);
                throw new RuntimeException(sourceName + "数据获取失败: " + msg);
            }

            log.debug("{}脚本返回结果: {}", sourceName, json);
            return json;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行{}脚本异常: {}", sourceName, e.getMessage(), e);
            throw new RuntimeException(sourceName + "数据获取异常: " + e.getMessage());
        }
    }
}
