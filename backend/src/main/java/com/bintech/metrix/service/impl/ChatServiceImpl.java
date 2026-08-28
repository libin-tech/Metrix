package com.bintech.metrix.service.impl;

import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.core.analysis.StockAdvisorPromptBuilder;
import com.bintech.metrix.dto.response.AnalysisResult;
import com.bintech.metrix.dto.response.ChatMessageVO;
import com.bintech.metrix.dto.response.ChatSessionVO;
import com.bintech.metrix.enums.ChatRole;
import com.bintech.metrix.repository.dao.ChatMessageDao;
import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.repository.entity.ChatMessage;
import com.bintech.metrix.repository.entity.ChatSession;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionService chatSessionService;
    private final ChatMessageDao messageDao;
    private final AiModelService aiModelService;
    private final StockAdvisorPromptBuilder promptBuilder;
    private final StockBasicDao stockBasicDao;
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

        Long messageCount = messageDao.countBySessionId(sessionId);
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
                userMsg.setUserId(userId);
                userMsg.setRole(ChatRole.USER);
                userMsg.setContent(content);
                userMsg.setTokens(content.length() / 2);
                userMsg.setCreateTime(LocalDateTime.now());
                messageDao.insert(userMsg);

                long t1 = System.currentTimeMillis();
                sendStep(emitter, "**Step 1/6** 🔍 解析股票名称...");
                StockBasic stockBasic = identifyStock(content, userId);
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
                    saveAssistantMessage(sessionId, userId, NO_STOCK_MSG, tokens, null, null, stepsJson);
                    chatSessionService.updateSessionTokenAndCount(sessionId, tokens);
                    return;
                }
                String stockCode = stockBasic.getTsCode();
                String stockName = stockBasic.getName();
                addStepRecord(stepRecords, 1, "解析股票名称：" + stockName, elapsed1, "completed");

                userMsg.setStockCode(stockCode);
                userMsg.setStockName(stockName);
                messageDao.updateById(userMsg);
                chatSessionService.updateSessionName(sessionId, "帮我分析下 " + stockName);
                sendStep(emitter, "✅ **Step 1/7** 🔍 解析股票名称：" + stockName);

                Map<String, Object> marketData = null;
                Map<String, Object> klinesData = null;
                Map<String, Object> newsData = null;
                Map<String, Object> chipData = null;
                Map<String, Object> topFreeShareholdersData = null;

                long t2 = System.currentTimeMillis();
                sendStep(emitter, "**Step 2/7** 📊 获取实时行情...");
                try {
                    marketData = marketDataService.fetchRealTimeData(stockBasic, userId);
                    sendStep(emitter, "✅ **Step 2/7** 📊 获取实时行情完成");
                    addStepRecord(stepRecords, 2, "获取实时行情", System.currentTimeMillis() - t2, "completed");
                } catch (Exception e) {
                    log.warn("获取实时行情失败: {}", e.getMessage());
                    sendStep(emitter, "⚠️ **Step 2/7** 📊 获取实时行情失败");
                    addStepRecord(stepRecords, 2, "获取实时行情", System.currentTimeMillis() - t2, "failed");
                }

                long t4 = System.currentTimeMillis();
                sendStep(emitter, "**Step 3/7** 📉 获取K线数据...");
                try {
                    klinesData = marketDataService.fetchKlinesData(stockBasic, 60, userId);
                    sendStep(emitter, "✅ **Step 3/7** 📉 获取K线数据完成");
                    addStepRecord(stepRecords, 3, "获取K线数据", System.currentTimeMillis() - t4, "completed");
                } catch (Exception e) {
                    log.warn("获取K线数据失败: {}", e.getMessage());
                    sendStep(emitter, "⚠️ **Step 3/7** 📉 获取K线数据失败");
                    addStepRecord(stepRecords, 3, "获取K线数据", System.currentTimeMillis() - t4, "failed");
                }

                long t5 = System.currentTimeMillis();
                sendStep(emitter, "**Step 4/7** 📰 获取新闻舆情...");
                try {
                    newsData = newsService.fetchStockNews(stockBasic, userId);
                    sendStep(emitter, "✅ **Step 4/7** 📰 获取新闻舆情完成");
                    addStepRecord(stepRecords, 4, "获取新闻舆情", System.currentTimeMillis() - t5, "completed");
                } catch (Exception e) {
                    log.warn("获取新闻数据失败: {}", e.getMessage());
                    sendStep(emitter, "⚠️ **Step 4/7** 📰 获取新闻舆情失败");
                    addStepRecord(stepRecords, 4, "获取新闻舆情", System.currentTimeMillis() - t5, "failed");
                }

                long t6 = System.currentTimeMillis();
                sendStep(emitter, "**Step 5/7** 📊 获取筹码分布...");
                try {
                    chipData = marketDataService.fetchChipData(stockBasic, userId);
                    sendStep(emitter, "✅ **Step 5/7** 📊 获取筹码分布完成");
                    addStepRecord(stepRecords, 5, "获取筹码分布", System.currentTimeMillis() - t6, "completed");
                } catch (Exception e) {
                    log.warn("获取筹码分布失败: {}", e.getMessage());
                    sendStep(emitter, "⚠️ **Step 5/7** 📊 获取筹码分布失败");
                    addStepRecord(stepRecords, 5, "获取筹码分布", System.currentTimeMillis() - t6, "failed");
                }

                long t7 = System.currentTimeMillis();
                sendStep(emitter, "**Step 6/7** 👤 获取股东数据...");
                try {
                    topFreeShareholdersData = marketDataService.fetchTopFreeShareholdersData(stockBasic, userId);
                    sendStep(emitter, "✅ **Step 6/7** 👤 获取股东数据完成");
                    addStepRecord(stepRecords, 6, "获取股东数据", System.currentTimeMillis() - t7, "completed");
                } catch (Exception e) {
                    log.warn("获取股东数据失败: {}", e.getMessage());
                    sendStep(emitter, "⚠️ **Step 6/7** 👤 获取股东数据失败");
                    addStepRecord(stepRecords, 6, "获取股东数据", System.currentTimeMillis() - t7, "failed");
                }

                long t8 = System.currentTimeMillis();
                sendStep(emitter, "**Step 7/7** 🤖 AI总结分析中...");

                String prompt = promptBuilder.build(content, stockBasic, marketData, newsData, klinesData, chipData, topFreeShareholdersData);

                aiModelService.generateAnalysisStreaming(prompt, modelType, userId,
                        partialResponse -> sendReport(emitter, partialResponse),
                        result -> completeChatAnalysis(emitter, sessionId, userId, stockCode, stockName,
                                messageCount, stepRecords, t8, result),
                        error -> failChatAnalysis(emitter, sessionId, messageCount, stepRecords, t8, error));
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

    private void sendReport(SseEmitter emitter, String partialResponse) {
        try {
            emitter.send(SseEmitter.event().name("report").data(partialResponse));
        } catch (IOException e) {
            log.warn("发送流式回答失败: {}", e.getMessage());
        }
    }

    private void completeChatAnalysis(SseEmitter emitter, Long sessionId, Long userId, String stockCode,
                                      String stockName, Long messageCount, List<Map<String, Object>> stepRecords,
                                      long startedAt, AnalysisResult result) {
        try {
            String content = result.getContent();
            int tokens = result.getTotalTokens();
            addStepRecord(stepRecords, 8, "AI总结分析", System.currentTimeMillis() - startedAt, "completed");
            String stepsJson = JSON_MAPPER.writeValueAsString(stepRecords);
            saveAssistantMessage(sessionId, userId, content, tokens, stockCode, stockName, stepsJson);
            chatSessionService.updateSessionTokenAndCount(sessionId, tokens);
            sendStep(emitter, "✅ **Step 8/8** 🤖 分析完成");
            emitter.send(SseEmitter.event().name("done").data("{\"sessionId\":" + sessionId
                    + ",\"content\":" + JSON_MAPPER.writeValueAsString(content)
                    + ",\"tokens\":" + tokens
                    + ",\"messageCount\":" + (messageCount / 2 + 1)
                    + ",\"steps\":" + stepsJson + "}"));
            emitter.complete();
        } catch (Exception e) {
            log.error("完成流式聊天分析失败", e);
            emitter.completeWithError(e);
        }
    }

    private void failChatAnalysis(SseEmitter emitter, Long sessionId, Long messageCount,
                                  List<Map<String, Object>> stepRecords, long startedAt, Throwable error) {
        log.error("AI分析出错: {}", error.getMessage(), error);
        addStepRecord(stepRecords, 8, "AI总结分析", System.currentTimeMillis() - startedAt, "failed");
        try {
            String stepsJson = JSON_MAPPER.writeValueAsString(stepRecords);
            emitter.send(SseEmitter.event().name("error").data(error.getMessage() == null ? "AI分析出错" : error.getMessage()));
            emitter.send(SseEmitter.event().name("done").data("{\"sessionId\":" + sessionId
                    + ",\"content\":\"\",\"tokens\":0"
                    + ",\"messageCount\":" + (messageCount / 2 + 1)
                    + ",\"steps\":" + stepsJson + "}"));
        } catch (IOException e) {
            log.error("发送流式分析错误事件失败", e);
        }
        emitter.completeWithError(error);
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
    protected void saveAssistantMessage(Long sessionId, Long userId, String content, int tokens,
                                         String stockCode, String stockName, String steps) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setUserId(userId);
        msg.setRole(ChatRole.ASSISTANT);
        msg.setContent(content);
        msg.setTokens(tokens);
        msg.setStockCode(stockCode);
        msg.setStockName(stockName);
        msg.setSteps(steps);
        msg.setCreateTime(LocalDateTime.now());
        messageDao.insert(msg);
    }

    private StockBasic identifyStock(String content, Long userId) {
        if (content == null || content.isBlank()) return null;
        String text = content.trim();

        String tsCodePattern = "\\d{6}\\.(SZ|SH)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(tsCodePattern).matcher(text);
        if (matcher.find()) {
            StockBasic stock = stockBasicDao.selectByTsCode(matcher.group().toUpperCase());
            if (stock != null) return stock;
        }

        String symbolPattern = "\\b\\d{6}\\b";
        matcher = java.util.regex.Pattern.compile(symbolPattern).matcher(text);
        if (matcher.find()) {
            log.info("识别股票代码: {}", matcher.group());
            StockBasic stock = stockBasicDao.selectBySymbol(matcher.group());
            if (stock != null) return stock;
        }

        StockBasic stock = identifyStockByAi(text, userId);
        if (stock != null) return stock;

        log.info("未找到股票: {}", text);
        return null;
    }

    private StockBasic identifyStockByAi(String text, Long userId) {
        String prompt = String.format(STOCK_IDENTIFY_PROMPT, text);
        String aiResult;
        try {
            aiResult = aiModelService.generateAnalysis(prompt, aiModelService.getActiveModelType(userId), userId);
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
        StockBasic stock = stockBasicDao.selectByName(stockName);
        if (stock != null) return stock;
        stock = stockBasicDao.selectByNameLike(stockName);
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
    public void deleteSession(Long id, Long userId) {
        chatSessionService.deleteSession(id, userId);
    }

    @Override
    public void deleteSessions(List<Long> ids, Long userId) {
        chatSessionService.deleteSessions(ids, userId);
    }

    @Override
    public List<ChatMessageVO> getSessionMessages(Long sessionId, Long userId) {
        chatSessionService.getSessionById(sessionId, userId);
        List<ChatMessage> messages = messageDao.selectBySessionIdOrderByCreateTimeAsc(sessionId);
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
