package com.bintech.metrix.service.impl;

import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.constants.ChatAgentConstants;
import com.bintech.metrix.core.analysis.ChatAgentIntentResolver;
import com.bintech.metrix.core.analysis.ChatAgentIntentResolver.Plan;
import com.bintech.metrix.core.analysis.ChatAnswerStream;
import com.bintech.metrix.core.analysis.MarketReviewDataFetcher;
import com.bintech.metrix.core.analysis.MarketReviewPromptBuilder;
import com.bintech.metrix.core.analysis.StockAdvisorPromptBuilder;
import com.bintech.metrix.dto.response.ChatMessageVO;
import com.bintech.metrix.dto.response.ChatSessionVO;
import com.bintech.metrix.enums.ChatRole;
import com.bintech.metrix.repository.dao.ChatMessageDao;
import com.bintech.metrix.repository.entity.ChatMessage;
import com.bintech.metrix.repository.entity.ChatSession;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.ChatService;
import com.bintech.metrix.service.ChatSessionService;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.NewsService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

/** 意图识别、范围校验和工具编排；分析继续由原问一问提示词构建器完成。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatSessionService chatSessionService;
    private final ChatMessageDao messageDao;
    private final AiModelService aiModelService;
    private final StockAdvisorPromptBuilder promptBuilder;
    private final ChatAgentIntentResolver intentResolver;
    private final MarketDataService marketDataService;
    private final NewsService newsService;
    private final MarketReviewDataFetcher marketReviewDataFetcher;
    private final MarketReviewPromptBuilder marketReviewPromptBuilder;
    private final ObjectMapper objectMapper;
    private final Map<Long, AgentRun> activeRuns = new ConcurrentHashMap<>();

    @RequiredArgsConstructor
    private static final class AgentRun {
        final Long sessionId;
        final Long userId;
        final SseEmitter emitter;
        final List<Map<String, Object>> steps = new ArrayList<>();
        final ChatAnswerStream answer = new ChatAnswerStream();
        volatile boolean finished;
        volatile Thread worker;
        String stockCode;
        String stockName;
        ChatMessage previousAnswer;
    }

    private record ConversationInput(String content, List<ChatMessage> history, ChatMessage previousAnswer) {}

    private ConversationInput prepareInput(String content, List<ChatMessage> history, boolean regenerate) {
        if (!regenerate) return new ConversationInput(content, history, null);
        if (history.size() < 2) throw new IllegalStateException("暂无可重新生成的回复。");
        ChatMessage previousAnswer = history.getLast();
        ChatMessage previousUser = history.get(history.size() - 2);
        if (previousAnswer.getRole() != ChatRole.ASSISTANT || previousUser.getRole() != ChatRole.USER) throw new IllegalStateException("请等待上一轮完成后重试。");
        return new ConversationInput(previousUser.getContent(), history.subList(0, history.size() - 2), previousAnswer);
    }

    @Override
    public SseEmitter sendMessage(Long sessionId, Long userId, String content) {
        return startRun(sessionId, userId, content, false);
    }

    @Override
    public SseEmitter regenerateMessage(Long sessionId, Long userId) {
        return startRun(sessionId, userId, null, true);
    }

    private SseEmitter startRun(Long sessionId, Long userId, String content, boolean regenerate) {
        chatSessionService.getSessionById(sessionId, userId);
        ConversationInput input = prepareInput(content, messageDao.selectBySessionIdOrderByCreateTimeAsc(sessionId), regenerate);
        long rounds = input.history().stream().filter(m -> m.getRole() == ChatRole.USER).count();
        if (rounds >= BusinessConstants.MAX_MESSAGES_PER_SESSION) throw new IllegalStateException("对话消息数量已达上限，请新建会话。");
        AgentRun run = new AgentRun(sessionId, userId, new SseEmitter(ChatAgentConstants.STREAM_TIMEOUT_MILLIS));
        run.previousAnswer = input.previousAnswer();
        if (activeRuns.putIfAbsent(sessionId, run) != null) throw new IllegalStateException("此会话正在生成，请先停止生成。");
        try {
            if (!regenerate) saveMessage(run, ChatRole.USER, input.content(), input.content().length() / 2);
        } catch (RuntimeException error) {
            activeRuns.remove(sessionId, run);
            throw error;
        }
        run.emitter.onTimeout(() -> finish(run, "error", "分析超时，请稍后重试。", 0));
        run.emitter.onError(error -> finish(run, "stopped", run.answer.content(), 0));
        run.emitter.onCompletion(() -> finish(run, "stopped", run.answer.content(), 0));
        run.worker = Thread.ofVirtual().name("chat-agent-" + sessionId).start(() -> execute(run, input.content(), input.history()));
        return run.emitter;
    }

    @Override
    public void stopGeneration(Long sessionId, Long userId) {
        chatSessionService.getSessionById(sessionId, userId);
        AgentRun run = activeRuns.get(sessionId);
        if (run == null) return;
        finish(run, "stopped", run.answer.content(), run.answer.content().length() / 2);
        if (run.worker != null) run.worker.interrupt();
    }

    private void execute(AgentRun run, String content, List<ChatMessage> history) {
        try {
            thinking(run, "intent", "正在识别分析意图与参数", "identifying");
            Plan plan = intentResolver.resolve(content, history, run.userId);
            checkRunning(run);
            switch (plan.intent()) {
                case REFUSE -> finish(run, "refused", ChatAgentConstants.REFUSAL, 0);
                case CLARIFY -> {
                    boolean alreadyAsked = !history.isEmpty() && history.getLast().getRole() == ChatRole.ASSISTANT
                            && history.getLast().getContent().equals(ChatAgentConstants.CLARIFICATION);
                    finish(run, alreadyAsked ? "refused" : "clarification",
                            alreadyAsked ? ChatAgentConstants.REFUSAL : ChatAgentConstants.CLARIFICATION, 0);
                }
                case ANALYZE -> analyze(run, content, plan);
            }
        } catch (CancellationException error) {
            log.info("问一问任务已停止: {}", run.sessionId);
        } catch (Exception error) {
            log.error("问一问Agent执行失败: sessionId={}", run.sessionId, error);
            finish(run, "error", "分析服务暂不可用，请检查模型和数据源配置后重试。", 0);
        }
    }

    /** 数据不足只报告缺失，不降级为无依据的通用聊天。 */
    private void analyze(AgentRun run, String content, Plan plan) {
        run.stockName = String.join("、", plan.targets());
        run.stockCode = String.join(",", plan.stocks().stream().map(StockBasic::getTsCode).toList());
        thinking(run, "scope", "已校验中国A股标的：" + run.stockName, "analyzing");
        thinking(run, "plan", "时间范围：" + plan.timeRange() + "；分析维度：" + String.join("、", plan.dimensions())
                + "。复用原问一问指标和研判方法；超出数据覆盖的维度将明确标注。", "analyzing");
        chatSessionService.updateSessionName(run.sessionId, run.stockName);
        String evidence = switch (plan.kind()) {
            case STOCK -> buildStockEvidence(run, content, plan.stocks());
            case SECTOR -> "板块研究仅覆盖本地行业分类中的样本：" + plan.targets()
                    + "。以下是各行业按证券代码排序取样、合计最多3只的样本，不是板块指数或龙头推荐，不可将样本表现推及整个行业。\n"
                    + buildStockEvidence(run, content, plan.stocks().stream().distinct().toList());
            case INDEX -> buildIndexEvidence(run, plan);
        };
        checkRunning(run);
        thinking(run, "generation", "公开数据获取完成，正在生成最终回复", "generating");
        String prompt = ChatAgentConstants.OUTPUT_RULES + "\n用户请求的参数：" + plan.timeRange() + " / " + plan.dimensions()
                + "\n" + evidence + "\n" + ChatAgentConstants.OUTPUT_RULES;
        aiModelService.generateAnalysisStreaming(prompt, aiModelService.getActiveModelType(run.userId), run.userId,
                chunk -> appendAnswer(run, chunk), result -> finish(run, "completed", run.answer.content(), result.getTotalTokens()),
                error -> {
                    log.error("问一问报告生成失败: {}", run.sessionId, error);
                    finish(run, "error", run.answer.content(), 0);
                }, () -> run.finished);
    }

    private String buildStockEvidence(AgentRun run, String content, List<StockBasic> stocks) {
        StringBuilder evidence = new StringBuilder();
        for (StockBasic stock : stocks) {
            Map<String, Object> market = tool(run, stock.getName() + " · 实时行情", () -> marketDataService.fetchRealTimeData(stock, run.userId));
            Map<String, Object> klines = tool(run, stock.getName() + " · 最近60条K线", () -> marketDataService.fetchKlinesData(stock, BusinessConstants.DEFAULT_KLINE_LIMIT, run.userId));
            Map<String, Object> news = tool(run, stock.getName() + " · 新闻与公告", () -> newsService.fetchStockNews(stock, run.userId));
            Map<String, Object> chip = tool(run, stock.getName() + " · 筹码分布", () -> marketDataService.fetchChipData(stock, run.userId));
            Map<String, Object> holders = tool(run, stock.getName() + " · 十大流通股东", () -> marketDataService.fetchTopFreeShareholdersData(stock, run.userId));
            evidence.append("已校验标的：").append(stock.getName()).append(" ").append(stock.getTsCode()).append("\n")
                    .append(promptBuilder.build(content, stock, market, news, klines, chip, holders)).append("\n\n");
        }
        return evidence.toString();
    }

    private String buildIndexEvidence(AgentRun run, Plan plan) {
        Map<String, String> selectedIndices = new LinkedHashMap<>();
        List<String> names = plan.targets().contains("A股大盘") ? MarketReviewDataFetcher.INDEX_NAMES : plan.targets();
        names.forEach(name -> selectedIndices.put(name, ChatAgentConstants.INDEX_SYMBOLS.get(name)));
        Map<String, Object> indices = tool(run, "A股指数行情与K线", () -> marketReviewDataFetcher.fetchIndexData(null, selectedIndices));
        Map<String, Object> turnover = tool(run, "沪深成交量与成交额", () -> marketReviewDataFetcher.fetchMarketTurnoverData(null));
        if (indices == null || turnover == null || !indices.keySet().containsAll(names)) return "请求的指数或成交额数据缺失，请在最终模板中说明暂时无法分析。";
        String dataDate = names.stream().map(name -> objectMapper.valueToTree(indices.get(name)).path("latest").path("date").asText())
                .filter(date -> !date.isBlank()).min(String::compareTo).orElse("未知日期");
        return "仅分析指定指数：" + plan.targets() + "；实际行情日期：" + dataDate + "\n"
                + marketReviewPromptBuilder.build(indices, turnover, dataDate, names);
    }

    private Map<String, Object> tool(AgentRun run, String title, Supplier<Map<String, Object>> fetcher) {
        thinking(run, "tool", "正在获取公开数据：" + title, "analyzing");
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> data = fetcher.get();
            checkRunning(run);
            boolean failed = data == null || data.isEmpty() || "error".equals(data.get(ApiConstants.KEY_STATUS)) || "failed".equals(data.get(ApiConstants.KEY_STATUS));
            toolResult(run, title, start, failed);
            return failed ? null : data;
        } catch (CancellationException error) {
            throw error;
        } catch (Exception error) {
            log.warn("Agent数据工具失败: {}", title, error);
            checkRunning(run);
            toolResult(run, title, start, true);
            return null;
        }
    }

    private void toolResult(AgentRun run, String title, long start, boolean failed) {
        synchronized (run) {
            checkRunning(run);
            Map<String, Object> step = Map.of("stage", "tool", "content", title + (failed ? "：数据缺失" : "：获取完成"),
                    "state", "analyzing", "status", failed ? "failed" : "completed", "elapsed", System.currentTimeMillis() - start);
            run.steps.add(step);
            send(run, "thinking", step);
        }
    }

    private void thinking(AgentRun run, String stage, String content, String state) {
        synchronized (run) {
            checkRunning(run);
            Map<String, Object> step = Map.of("stage", stage, "content", content, "state", state);
            run.steps.add(step);
            send(run, "thinking", step);
        }
    }

    private void appendAnswer(AgentRun run, String chunk) {
        synchronized (run) {
            if (run.finished) return;
            String delta = run.answer.append(chunk);
            if (!delta.isEmpty()) send(run, "final", Map.of("content", delta, "delta", true));
        }
    }

    /** 一次终止仅持久化一次；停止、断连、超时和模型回调共用同一终态。 */
    private void finish(AgentRun run, String status, String content, int tokens) {
        synchronized (run) {
            if (run.finished) return;
            run.finished = true;
            String answer = content;
            if ("completed".equals(status) && answer.isBlank()) {
                status = "error";
                answer = "模型未返回符合格式的分析报告，请重新生成。";
            }
            if (!"clarification".equals(status) && !answer.contains(ChatAgentConstants.DISCLAIMER)) {
                answer += "\n\n> " + ChatAgentConstants.DISCLAIMER;
            }
            run.steps.add(Map.of("stage", "complete", "state", status, "content", status));
            try {
                saveMessage(run, ChatRole.ASSISTANT, answer, tokens);
                if (run.previousAnswer == null) chatSessionService.updateSessionTokenAndCount(run.sessionId, tokens);
                else chatSessionService.addSessionTokens(run.sessionId, tokens);
                send(run, "final", Map.of("content", answer, "delta", false, "status", status));
                send(run, "done", Map.of("status", status, "sessionId", run.sessionId, "tokens", tokens, "steps", run.steps));
                run.emitter.complete();
            } catch (Exception error) {
                log.error("Agent终态保存失败: {}", run.sessionId, error);
                run.emitter.completeWithError(error);
            } finally {
                activeRuns.remove(run.sessionId, run);
            }
        }
    }

    private void saveMessage(AgentRun run, ChatRole role, String content, int tokens) {
        ChatMessage msg = role == ChatRole.ASSISTANT && run.previousAnswer != null ? run.previousAnswer : new ChatMessage();
        msg.setSessionId(run.sessionId);
        msg.setUserId(run.userId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setTokens(tokens);
        msg.setCreator(run.userId.toString());
        msg.setModifier(run.userId.toString());
        msg.setStockCode(run.stockCode);
        msg.setStockName(run.stockName);
        if (msg.getId() == null) msg.setCreateTime(LocalDateTime.now());
        if (role == ChatRole.ASSISTANT) msg.setSteps(objectMapper.writeValueAsString(run.steps));
        int affected = msg.getId() == null ? messageDao.insert(msg) : messageDao.updateById(msg);
        if (affected == 0) throw new IllegalStateException("会话消息保存失败，请重新加载会话后重试。");
    }

    private void checkRunning(AgentRun run) {
        if (run.finished || Thread.currentThread().isInterrupted()) throw new CancellationException();
    }

    private void send(AgentRun run, String event, Map<String, Object> data) {
        try {
            run.emitter.send(SseEmitter.event().name(event).data(objectMapper.writeValueAsString(data)));
        } catch (IOException | IllegalStateException error) {
            log.debug("Agent连接已关闭: {}", run.sessionId, error);
            if (!run.finished) finish(run, "stopped", run.answer.content(), 0);
        }
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
