package com.bintech.metrix.service.impl;

import com.bintech.metrix.constants.ChatAgentConstants;
import com.bintech.metrix.core.analysis.ChatAgentIntentResolver;
import com.bintech.metrix.core.analysis.MarketReviewDataFetcher;
import com.bintech.metrix.core.analysis.MarketReviewPromptBuilder;
import com.bintech.metrix.core.analysis.StockAdvisorPromptBuilder;
import com.bintech.metrix.dto.response.AnalysisResult;
import com.bintech.metrix.enums.ChatRole;
import com.bintech.metrix.repository.dao.ChatMessageDao;
import com.bintech.metrix.repository.entity.ChatMessage;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.ChatSessionService;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.NewsService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatServiceImplTest {
    private final ChatSessionService sessions = mock(ChatSessionService.class);
    private final ChatMessageDao messages = mock(ChatMessageDao.class);
    private final AiModelService ai = mock(AiModelService.class);
    private final StockAdvisorPromptBuilder prompts = mock(StockAdvisorPromptBuilder.class);
    private final ChatAgentIntentResolver resolver = mock(ChatAgentIntentResolver.class);
    private final MarketDataService market = mock(MarketDataService.class);
    private final NewsService news = mock(NewsService.class);
    private final List<ChatMessage> history = new CopyOnWriteArrayList<>();
    private final BlockingQueue<ChatMessage> replies = new LinkedBlockingQueue<>();
    private final ChatServiceImpl service = new ChatServiceImpl(sessions, messages, ai, prompts, resolver, market, news,
            mock(MarketReviewDataFetcher.class), mock(MarketReviewPromptBuilder.class), new ObjectMapper());

    ChatServiceImplTest() {
        when(messages.selectBySessionIdOrderByCreateTimeAsc(1L)).thenAnswer(call -> List.copyOf(history));
        when(messages.insert(any())).thenAnswer(call -> {
            ChatMessage msg = call.getArgument(0);
            msg.setId((long) history.size() + 1);
            history.add(msg);
            if (msg.getRole() == ChatRole.ASSISTANT) replies.add(msg);
            return 1;
        });
    }

    @Test
    void regenerationReplacesTheLastAnswerWithoutAddingAnotherUserTurn() throws Exception {
        prepareAnalysis();
        when(messages.updateById(any())).thenAnswer(call -> { replies.add(call.getArgument(0)); return 1; });
        doAnswer(call -> {
            Consumer<String> next = call.getArgument(3);
            Consumer<AnalysisResult> complete = call.getArgument(4);
            next.accept("## 结论摘要\n公开分析");
            complete.accept(new AnalysisResult("report", 42));
            return null;
        }).when(ai).generateAnalysisStreaming(anyString(), any(), eq(1L), any(), any(), any(), any());
        service.sendMessage(1L, 1L, "分析贵州茅台");
        ChatMessage original = awaitReply();
        service.stopGeneration(1L, 1L);
        service.regenerateMessage(1L, 1L);
        assertEquals(original.getId(), awaitReply().getId());
        assertEquals(2, history.size());
        verify(sessions, timeout(2000)).addSessionTokens(1L, 42);
        verify(sessions, times(1)).updateSessionTokenAndCount(1L, 42);
    }

    @Test
    void refusesWithoutFetchingAnyFinancialData() throws Exception {
        classify(ChatAgentIntentResolver.Intent.REFUSE);
        service.sendMessage(1L, 1L, "美股走势");
        ChatMessage reply = awaitReply();
        assertTrue(reply.getContent().contains("暂不支持北交所"));
        assertTrue(reply.getContent().contains(ChatAgentConstants.DISCLAIMER));
        verifyNoInteractions(market, news, ai, prompts);
    }

    @Test
    void asksOnceThenRefusesIfStillUnresolved() throws Exception {
        classify(ChatAgentIntentResolver.Intent.CLARIFY);
        service.sendMessage(1L, 1L, "分析那家公司");
        assertEquals(ChatAgentConstants.CLARIFICATION, awaitReply().getContent());
        service.stopGeneration(1L, 1L);
        service.sendMessage(1L, 1L, "就是那家");
        assertTrue(awaitReply().getContent().startsWith("抱歉"));
        verifyNoInteractions(market, news, ai, prompts);
    }

    @Test
    void completesWithOnlySanitizedFinalContentAndDisclaimer() throws Exception {
        prepareAnalysis();
        doAnswer(call -> {
            Consumer<String> next = call.getArgument(3);
            Consumer<AnalysisResult> complete = call.getArgument(4);
            next.accept("<think>私密内容</think>## 结论摘要\n公开分析");
            complete.accept(new AnalysisResult("raw private result", 42));
            return null;
        }).when(ai).generateAnalysisStreaming(anyString(), any(), eq(1L), any(), any(), any(), any());
        service.sendMessage(1L, 1L, "分析贵州茅台");
        ChatMessage reply = awaitReply();
        assertTrue(reply.getContent().startsWith("## 结论摘要"));
        assertFalse(reply.getContent().contains("私密"));
        assertFalse(reply.getContent().contains("raw private"));
        assertTrue(reply.getContent().endsWith(ChatAgentConstants.DISCLAIMER));
        verify(market).fetchKlinesData(any(), eq(60), eq(1L));
        verify(prompts).build(anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void stoppingBeforeTheFirstAnswerDoesNotPersistOnlyADisclaimer() throws Exception {
        prepareAnalysis();
        CountDownLatch started = new CountDownLatch(1);
        doAnswer(call -> { started.countDown(); return null; }).when(ai)
                .generateAnalysisStreaming(anyString(), any(), eq(1L), any(), any(), any(), any());
        service.sendMessage(1L, 1L, "分析贵州茅台");
        assertTrue(started.await(3, TimeUnit.SECONDS));
        service.stopGeneration(1L, 1L);
        ChatMessage reply = awaitReply();
        assertFalse(reply.getContent().replace(ChatAgentConstants.DISCLAIMER, "").replace(">", "").isBlank(),
                "An interrupted analysis must explain that no report was generated");
    }

    @Test
    void preservesAReportWithANumberedSummaryHeading() throws Exception {
        prepareAnalysis();
        doAnswer(call -> {
            Consumer<String> next = call.getArgument(3);
            Consumer<AnalysisResult> complete = call.getArgument(4);
            String report = "## 一、结论摘要\n公开分析\n## 分析依据\n公开数据\n## 风险提示\n风险";
            next.accept(report);
            complete.accept(new AnalysisResult(report, 42));
            return null;
        }).when(ai).generateAnalysisStreaming(anyString(), any(), eq(1L), any(), any(), any(), any());
        service.sendMessage(1L, 1L, "分析贵州茅台");
        assertTrue(awaitReply().getContent().contains("公开分析"));
    }

    @Test
    void modelFailureBeforeTheFirstAnswerExplainsTheFailure() throws Exception {
        prepareAnalysis();
        doAnswer(call -> {
            Consumer<Throwable> error = call.getArgument(5);
            error.accept(new IllegalStateException("fixture model failure"));
            return null;
        }).when(ai).generateAnalysisStreaming(anyString(), any(), eq(1L), any(), any(), any(), any());
        service.sendMessage(1L, 1L, "分析贵州茅台");
        ChatMessage reply = awaitReply();
        assertTrue(reply.getContent().contains("分析报告生成失败"));
        assertTrue(reply.getSteps().contains("\"state\":\"error\""));
    }

    @Test
    void completionWithoutAPublishableReportIsAnError() throws Exception {
        prepareAnalysis();
        doAnswer(call -> {
            Consumer<String> next = call.getArgument(3);
            Consumer<AnalysisResult> complete = call.getArgument(4);
            next.accept("<think>私密推理</think>");
            complete.accept(new AnalysisResult("<think>私密推理</think>", 42));
            return null;
        }).when(ai).generateAnalysisStreaming(anyString(), any(), eq(1L), any(), any(), any(), any());
        service.sendMessage(1L, 1L, "分析贵州茅台");
        ChatMessage reply = awaitReply();
        assertTrue(reply.getContent().contains("模型未返回符合格式的分析报告"));
        assertFalse(reply.getContent().contains("私密推理"));
        assertTrue(reply.getSteps().contains("\"state\":\"error\""));
    }

    @Test
    void stopPersistsOnceAndLateModelCallbacksCannotOverwriteIt() throws Exception {
        prepareAnalysis();
        AtomicReference<Consumer<String>> next = new AtomicReference<>();
        AtomicReference<Consumer<AnalysisResult>> complete = new AtomicReference<>();
        AtomicReference<BooleanSupplier> cancelled = new AtomicReference<>();
        CountDownLatch started = new CountDownLatch(1);
        doAnswer(call -> {
            next.set(call.getArgument(3)); complete.set(call.getArgument(4)); cancelled.set(call.getArgument(6)); started.countDown(); return null;
        }).when(ai).generateAnalysisStreaming(anyString(), any(), eq(1L), any(), any(), any(), any());
        service.sendMessage(1L, 1L, "分析贵州茅台");
        assertTrue(started.await(3, TimeUnit.SECONDS));
        assertThrows(IllegalStateException.class, () -> service.sendMessage(1L, 1L, "重复并发请求"));
        next.get().accept("## 结论摘要\n部分回复");
        service.stopGeneration(1L, 1L);
        ChatMessage reply = awaitReply();
        assertTrue(cancelled.get().getAsBoolean());
        next.get().accept("不应显示的迟到内容");
        complete.get().accept(new AnalysisResult("迟到回复", 123));
        assertFalse(reply.getContent().contains("迟到"));
        assertTrue(reply.getSteps().contains("stopped"));
        assertEquals(1, history.stream().filter(msg -> msg.getRole() == ChatRole.ASSISTANT).count());
    }

    @Test
    void doesNotStartATaskWhenOwnershipCheckFails() {
        doThrow(new IllegalStateException("无权访问")).when(sessions).getSessionById(1L, 2L);
        assertThrows(IllegalStateException.class, () -> service.sendMessage(1L, 2L, "分析贵州茅台"));
        verifyNoInteractions(messages, resolver, market, ai);
    }

    private void classify(ChatAgentIntentResolver.Intent intent) throws Exception {
        when(resolver.resolve(anyString(), anyList(), eq(1L))).thenReturn(new ChatAgentIntentResolver.Plan(intent,
                ChatAgentIntentResolver.Kind.STOCK, List.of(), List.of(), "", List.of()));
    }
    private void prepareAnalysis() throws Exception {
        StockBasic stock = new StockBasic(); stock.setName("贵州茅台"); stock.setTsCode("600519.SH");
        when(resolver.resolve(anyString(), anyList(), eq(1L))).thenReturn(new ChatAgentIntentResolver.Plan(ChatAgentIntentResolver.Intent.ANALYZE,
                ChatAgentIntentResolver.Kind.STOCK, List.of("贵州茅台"), List.of(stock), "最近60个交易日", List.of("风险")));
        Map<String, Object> data = Map.of("status", "success", "data", List.of("public"));
        when(market.fetchRealTimeData(any(), eq(1L))).thenReturn(data);
        when(market.fetchKlinesData(any(), anyInt(), eq(1L))).thenReturn(data);
        when(market.fetchChipData(any(), eq(1L))).thenReturn(data);
        when(market.fetchTopFreeShareholdersData(any(), eq(1L))).thenReturn(data);
        when(news.fetchStockNews(any(), eq(1L))).thenReturn(data);
        when(prompts.build(anyString(), any(), any(), any(), any(), any(), any())).thenReturn("原分析方法");
    }
    private ChatMessage awaitReply() throws InterruptedException {
        ChatMessage reply = replies.poll(3, TimeUnit.SECONDS);
        assertNotNull(reply, "Agent should persist a terminal reply");
        return reply;
    }
}
