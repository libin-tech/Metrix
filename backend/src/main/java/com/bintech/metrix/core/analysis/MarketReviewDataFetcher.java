package com.bintech.metrix.core.analysis;

import cn.hutool.json.JSONObject;
import com.bintech.metrix.constants.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MarketReviewDataFetcher {

    public static final List<String> INDEX_SYMBOLS = List.of("sh000001", "sz399001", "sz399006", "sh000688");
    public static final List<String> INDEX_NAMES = List.of("上证指数", "深证成指", "创业板指", "科创50");
    private static final int SCRIPT_TIMEOUT_SECONDS = 120;

    @Value("${python.executable:python}")
    private String pythonExecutable;

    @Value("${python.akshare-script-path:python-service/akshare.py}")
    private String akshareScriptPath;

    /**
     * 通过调用 AKShare Python 脚本获取四大指数（上证/深证/创业板/科创50）的行情和K线数据
     */
    public Map<String, Object> fetchIndexData(String reviewDate) {
        String symbols = String.join(",", INDEX_SYMBOLS);
        String scriptPath = akshareScriptPath.replace("akshare.py", "akshare_index.py");

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);
        command.add("--symbols");
        command.add(symbols);

        if (reviewDate != null && !reviewDate.isBlank()) {
            command.add("--date");
            command.add(reviewDate);
        }

        log.info("执行AKShare指数脚本: {}", String.join(" ", command));
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder outputBuilder = new StringBuilder();
            Thread reader = Thread.ofVirtual()
                    .name("index-data-reader")
                    .start(() -> {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                outputBuilder.append(line).append('\n');
                            }
                        } catch (Exception e) {
                            log.warn("读取指数脚本输出流异常: {}", e.getMessage());
                        }
                    });
            boolean finished = process.waitFor(SCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            reader.join(SystemConstants.READER_JOIN_TIMEOUT_MILLIS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("指数脚本执行超时");
            }
            String output = outputBuilder.toString().trim();
            if (output.isEmpty()) {
                throw new RuntimeException("指数脚本输出为空");
            }
            JSONObject json = new JSONObject(output);
            if (!"success".equals(json.getStr("status"))) {
                throw new RuntimeException("指数数据获取失败: " + json.getStr("message"));
            }
            JSONObject data = json.getJSONObject("data");
            Map<String, Object> result = new HashMap<>();
            for (int i = 0; i < INDEX_SYMBOLS.size(); i++) {
                String sym = INDEX_SYMBOLS.get(i);
                String name = INDEX_NAMES.get(i);
                JSONObject idxData = data.getJSONObject(sym);
                if (idxData != null) {
                    Map<String, Object> idxMap = new HashMap<>();
                    idxMap.put("name", name);
                    idxMap.put("changePct", idxData.getDouble("changePct", 0.0));
                    idxMap.put("latest", idxData.getJSONObject("latest"));
                    idxMap.put("records", idxData.getJSONArray("records"));
                    result.put(name, idxMap);
                }
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("获取指数数据异常: " + e.getMessage());
        }
    }
}
