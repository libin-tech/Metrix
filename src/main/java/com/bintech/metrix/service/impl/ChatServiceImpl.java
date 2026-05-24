package com.bintech.metrix.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.core.analysis.StockAdvisorPromptBuilder;
import com.bintech.metrix.dto.response.ChatMessageVO;
import com.bintech.metrix.dto.response.ChatSessionVO;
import com.bintech.metrix.repository.entity.ChatMessage;
import com.bintech.metrix.repository.entity.ChatSession;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.repository.mapper.ChatMessageMapper;
import com.bintech.metrix.repository.mapper.StockBasicMapper;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.ChatService;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.NewsService;
import com.bintech.metrix.service.StockBasicService;
import com.bintech.metrix.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionService chatSessionService;
    private final ChatMessageMapper messageMapper;
    private final AiModelService aiModelService;
    private final StockAdvisorPromptBuilder promptBuilder;
    private final StockBasicMapper stockBasicMapper;
    private final MarketDataService marketDataService;
    private final NewsService newsService;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final String NO_STOCK_MSG = "很抱歉，暂时无法识别到您需要分析的股票。请更换问题后重试。";

    private static final String STOCK_IDENTIFY_PROMPT = """
            你是一个A股股票识别专家。从用户的问题中，识别出用户想要查询的A股股票名称。

            规则：
            - 如果用户明确提到了某只A股股票的名称或代码，返回该股票的完整名称
            - 如果用户只提到了股票简称（如"茅台"对应"贵州茅台"），返回完整的官方股票名称
            - 如果无法识别到任何具体的A股股票，只返回null（不带引号）
            - 如果用户提到了多只股票，返回最相关的一只
            - 只返回股票名称，不要返回任何其他内容

            用户：%s
            返回：""";

    @Override
    public SseEmitter sendMessage(Long sessionId, Long userId, String content) {
        ChatSession session = chatSessionService.getSessionById(sessionId);
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此对话");
        }

        Long messageCount = messageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId));
        if (messageCount >= BusinessConstants.MAX_MESSAGES_PER_SESSION * 2) {
            throw new RuntimeException("对话消息数量已达上限（" + BusinessConstants.MAX_MESSAGES_PER_SESSION + "轮）");
        }

        String modelType = aiModelService.getActiveModelType();
        SseEmitter emitter = new SseEmitter(0L);

        Thread.ofVirtual().name("chat-pipeline-" + sessionId).start(() -> {
            List<Map<String, Object>> stepRecords = new ArrayList<>();
            try {
                ChatMessage userMsg = new ChatMessage();
                userMsg.setSessionId(sessionId);
                userMsg.setRole("user");
                userMsg.setContent(content);
                userMsg.setTokens(content.length() / 2);
                userMsg.setCreateTime(LocalDateTime.now());
                messageMapper.insert(userMsg);

                long t1 = System.currentTimeMillis();
                sendStep(emitter, "**Step 1/6** 🔍 解析股票名称...");
                StockBasic stockBasic = identifyStock(content);
                long elapsed1 = System.currentTimeMillis() - t1;
                if (stockBasic == null) {
                    chatSessionService.updateSessionName(sessionId, "未知任务");
                    addStepRecord(stepRecords, 1, "解析股票名称", elapsed1, "failed");
                    sendStep(emitter, "❌ 未识别到A股股票");
                    int tokens = NO_STOCK_MSG.length() / 2;
                    String stepsJson = JSON_MAPPER.writeValueAsString(stepRecords);
                    emitter.send(SseEmitter.event().name("done")
                            .data("{\"sessionId\":" + sessionId
                                    + ",\"content\":" + JSON_MAPPER.writeValueAsString(NO_STOCK_MSG)
                                    + ",\"tokens\":" + tokens
                                    + ",\"messageCount\":" + (messageCount / 2 + 1)
                                    + ",\"steps\":" + stepsJson
                                    + "}"));
                    emitter.complete();
                    saveAssistantMessage(sessionId, NO_STOCK_MSG, tokens, null, null, stepsJson);
                    chatSessionService.updateSessionTokenAndCount(sessionId, tokens);
                    return;
                }
                String stockCode = stockBasic.getTsCode();
                String stockName = stockBasic.getName();
                addStepRecord(stepRecords, 1, "解析股票名称：" + stockName, elapsed1, "completed");

                userMsg.setStockCode(stockCode);
                userMsg.setStockName(stockName);
                messageMapper.updateById(userMsg);
                chatSessionService.updateSessionName(sessionId, "帮我分析下 " + stockName);
                sendStep(emitter, "✅ **Step 1/6** 🔍 解析股票名称：" + stockName);

                Map<String, Object> marketData = null;
                Map<String, Object> depthData = null;
                Map<String, Object> klinesData = null;
                Map<String, Object> newsData = null;
                Map<String, Object> chipData = null;
                Map<String, Object> topFreeShareholdersData = null;

                long t2 = System.currentTimeMillis();
                sendStep(emitter, "**Step 2/8** 📊 获取实时行情...");
                try {
                    marketData = marketDataService.fetchRealTimeData(stockBasic);
                    sendStep(emitter, "✅ **Step 2/8** 📊 获取实时行情完成");
                    addStepRecord(stepRecords, 2, "获取实时行情", System.currentTimeMillis() - t2, "completed");
                } catch (Exception e) {
                    log.warn("获取实时行情失败: {}", e.getMessage());
                    sendStep(emitter, "⚠️ **Step 2/8** 📊 获取实时行情失败");
                    addStepRecord(stepRecords, 2, "获取实时行情", System.currentTimeMillis() - t2, "failed");
                }

                long t3 = System.currentTimeMillis();
                sendStep(emitter, "**Step 3/8** 📈 获取深度行情...");
                try {
                    depthData = marketDataService.fetchDepthData(stockBasic);
                    sendStep(emitter, "✅ **Step 3/8** 📈 获取深度行情完成");
                    addStepRecord(stepRecords, 3, "获取深度行情", System.currentTimeMillis() - t3, "completed");
                } catch (Exception e) {
                    log.warn("获取深度数据失败: {}", e.getMessage());
                    sendStep(emitter, "⚠️ **Step 3/8** 📈 获取深度行情失败");
                    addStepRecord(stepRecords, 3, "获取深度行情", System.currentTimeMillis() - t3, "failed");
                }

                long t4 = System.currentTimeMillis();
                sendStep(emitter, "**Step 4/8** 📉 获取K线数据...");
                try {
                    klinesData = marketDataService.fetchKlinesData(stockBasic, 60);
                    sendStep(emitter, "✅ **Step 4/8** 📉 获取K线数据完成");
                    addStepRecord(stepRecords, 4, "获取K线数据", System.currentTimeMillis() - t4, "completed");
                } catch (Exception e) {
                    log.warn("获取K线数据失败: {}", e.getMessage());
                    sendStep(emitter, "⚠️ **Step 4/8** 📉 获取K线数据失败");
                    addStepRecord(stepRecords, 4, "获取K线数据", System.currentTimeMillis() - t4, "failed");
                }

                long t5 = System.currentTimeMillis();
                sendStep(emitter, "**Step 5/8** 📰 获取新闻舆情...");
                try {
                    newsData = newsService.fetchStockNews(stockBasic);
                    sendStep(emitter, "✅ **Step 5/8** 📰 获取新闻舆情完成");
                    addStepRecord(stepRecords, 5, "获取新闻舆情", System.currentTimeMillis() - t5, "completed");
                } catch (Exception e) {
                    log.warn("获取新闻数据失败: {}", e.getMessage());
                    sendStep(emitter, "⚠️ **Step 5/8** 📰 获取新闻舆情失败");
                    addStepRecord(stepRecords, 5, "获取新闻舆情", System.currentTimeMillis() - t5, "failed");
                }

                long t6 = System.currentTimeMillis();
                sendStep(emitter, "**Step 6/8** 📊 获取筹码分布...");
                try {
                    chipData = marketDataService.fetchChipData(stockBasic);
                    sendStep(emitter, "✅ **Step 6/8** 📊 获取筹码分布完成");
                    addStepRecord(stepRecords, 6, "获取筹码分布", System.currentTimeMillis() - t6, "completed");
                } catch (Exception e) {
                    log.warn("获取筹码分布失败: {}", e.getMessage());
                    sendStep(emitter, "⚠️ **Step 6/8** 📊 获取筹码分布失败");
                    addStepRecord(stepRecords, 6, "获取筹码分布", System.currentTimeMillis() - t6, "failed");
                }

                long t7 = System.currentTimeMillis();
                sendStep(emitter, "**Step 7/8** 👤 获取股东数据...");
                try {
                    topFreeShareholdersData = marketDataService.fetchTopFreeShareholdersData(stockBasic);
                    sendStep(emitter, "✅ **Step 7/8** 👤 获取股东数据完成");
                    addStepRecord(stepRecords, 7, "获取股东数据", System.currentTimeMillis() - t7, "completed");
                } catch (Exception e) {
                    log.warn("获取股东数据失败: {}", e.getMessage());
                    sendStep(emitter, "⚠️ **Step 7/8** 👤 获取股东数据失败");
                    addStepRecord(stepRecords, 7, "获取股东数据", System.currentTimeMillis() - t7, "failed");
                }

                long t8 = System.currentTimeMillis();
                sendStep(emitter, "**Step 8/8** 🤖 AI总结分析中...");

                String prompt = promptBuilder.build(content, stockBasic, marketData, depthData, newsData, klinesData, chipData, topFreeShareholdersData);

                try {
                    long aiStartTime = System.currentTimeMillis();
                    String aiContent = aiModelService.generateAnalysis(prompt, modelType);
                    long aiElapsed = System.currentTimeMillis() - aiStartTime;
                    int tokens = (int) Math.ceil(aiContent.length() / 2.0);

                    addStepRecord(stepRecords, 8, "AI总结分析", aiElapsed, "completed");
                    String stepsJson = JSON_MAPPER.writeValueAsString(stepRecords);

                    saveAssistantMessage(sessionId, aiContent, tokens, stockCode, stockName, stepsJson);
                    chatSessionService.updateSessionTokenAndCount(sessionId, tokens);

                    sendStep(emitter, "✅ **Step 8/8** 🤖 分析完成");
                    emitter.send(SseEmitter.event()
                            .name("done")
                            .data("{\"sessionId\":" + sessionId
                                    + ",\"content\":" + JSON_MAPPER.writeValueAsString(aiContent)
                                    + ",\"tokens\":" + tokens
                                    + ",\"messageCount\":" + (messageCount / 2 + 1)
                                    + ",\"steps\":" + stepsJson
                                    + "}"));
                    emitter.complete();
                } catch (Exception e) {
                    log.error("AI分析出错: {}", e.getMessage());
                    long aiElapsed = System.currentTimeMillis() - t8;
                    addStepRecord(stepRecords, 8, "AI总结分析", aiElapsed, "failed");
                    String stepsJson = JSON_MAPPER.writeValueAsString(stepRecords);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(e.getMessage() != null ? e.getMessage() : "AI分析出错"));
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data("{\"sessionId\":" + sessionId
                                        + ",\"content\":\"\"" 
                                        + ",\"tokens\":0"
                                        + ",\"messageCount\":" + (messageCount / 2 + 1)
                                        + ",\"steps\":" + stepsJson
                                        + "}"));
                    } catch (IOException ex) {
                        log.error("发送错误事件失败", ex);
                    }
                    emitter.completeWithError(e);
                }
            } catch (Exception e) {
                log.error("管道处理异常: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("系统处理异常: " + e.getMessage()));
                } catch (IOException ex) {
                    log.error("发送异常事件失败", ex);
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void sendStep(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().name("step").data(text));
        } catch (IOException e) {
            log.warn("发送进度事件失败", e);
        }
    }

    private void addStepRecord(List<Map<String, Object>> records, int stepNum, String title, long elapsed, String status) {
        Map<String, Object> record = new HashMap<>();
        record.put("step", stepNum);
        record.put("title", title);
        record.put("elapsed", elapsed);
        record.put("status", status);
        records.add(record);
    }

    @Transactional
    protected void saveAssistantMessage(Long sessionId, String content, int tokens,
                                         String stockCode, String stockName, String steps) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole("assistant");
        msg.setContent(content);
        msg.setTokens(tokens);
        msg.setStockCode(stockCode);
        msg.setStockName(stockName);
        msg.setSteps(steps);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
    }

    private StockBasic identifyStock(String content) {
        if (content == null || content.isBlank()) return null;
        String text = content.trim();

        String tsCodePattern = "\\d{6}\\.(SZ|SH)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(tsCodePattern).matcher(text);
        if (matcher.find()) {
            StockBasic stock = stockBasicMapper.selectOne(
                    new LambdaQueryWrapper<StockBasic>()
                            .eq(StockBasic::getTsCode, matcher.group().toUpperCase()));
            if (stock != null) return stock;
        }

        String symbolPattern = "\\b\\d{6}\\b";
        matcher = java.util.regex.Pattern.compile(symbolPattern).matcher(text);
        if (matcher.find()) {
            log.info("识别股票代码: {}", matcher.group());
            StockBasic stock = stockBasicMapper.selectOne(
                    new LambdaQueryWrapper<StockBasic>()
                            .eq(StockBasic::getSymbol, matcher.group()));
            if (stock != null) return stock;
        }

        StockBasic stock = identifyStockByAi(text);
        if (stock != null) return stock;

        log.info("未找到股票: {}", text);
        return null;
    }

    private StockBasic identifyStockByAi(String text) {
        String prompt = String.format(STOCK_IDENTIFY_PROMPT, text);
        String aiResult;
        try {
            aiResult = aiModelService.generateAnalysis(prompt, aiModelService.getActiveModelType());
        } catch (Exception e) {
            log.warn("AI识别股票失败: {}", e.getMessage());
            return null;
        }
        if (aiResult == null || aiResult.isBlank() || "null".equals(aiResult.trim())) {
            log.info("AI未识别到股票，用户问题: {}", text);
            return null;
        }
        String stockName = aiResult.trim();
        log.info("AI识别股票名称: {}", stockName);
        StockBasic stock = stockBasicMapper.selectOne(
                new LambdaQueryWrapper<StockBasic>()
                        .eq(StockBasic::getName, stockName)
                        .last("LIMIT 1"));
        if (stock != null) return stock;
        stock = stockBasicMapper.selectOne(
                new LambdaQueryWrapper<StockBasic>()
                        .like(StockBasic::getName, stockName)
                        .last("LIMIT 1"));
        if (stock != null) return stock;
        log.warn("AI识别到股票名称[{}]但数据库未匹配到", stockName);
        return null;
    }

    @Override
    public List<ChatSessionVO> listSessions(Long userId) {
        return chatSessionService.getUserSessions(userId);
    }

    @Override
    public ChatSessionVO createSession(Long userId, String sessionName) {
        ChatSession session = chatSessionService.createSession(userId, sessionName);
        return ChatSessionVO.builder()
                .id(session.getId())
                .sessionName(session.getSessionName())
                .userId(session.getUserId())
                .totalTokens(session.getTotalTokens())
                .messageCount(session.getMessageCount())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    @Override
    public void deleteSession(Long id) {
        chatSessionService.deleteSession(id);
    }

    @Override
    public void deleteSessions(List<Long> ids, Long userId) {
        chatSessionService.deleteSessions(ids, userId);
    }

    @Override
    public List<ChatMessageVO> getSessionMessages(Long sessionId) {
        List<ChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime));
        return messages.stream()
                .map(m -> ChatMessageVO.builder()
                        .id(m.getId())
                        .sessionId(m.getSessionId())
                        .role(m.getRole())
                        .content(m.getContent())
                        .tokens(m.getTokens())
                        .stockCode(m.getStockCode())
                        .stockName(m.getStockName())
                        .createTime(m.getCreateTime())
                        .steps(m.getSteps())
                        .build())
                .toList();
    }
}
