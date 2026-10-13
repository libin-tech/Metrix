package com.bintech.metrix.integration;

import com.bintech.metrix.dto.request.ChatSendRequest;
import com.bintech.metrix.dto.response.ChatMessageVO;
import com.bintech.metrix.dto.response.ChatSessionVO;
import com.bintech.metrix.repository.dao.MarketDataConfigDao;
import com.bintech.metrix.repository.dao.NewsSourceConfigDao;
import com.bintech.metrix.repository.dao.UserDao;
import com.bintech.metrix.repository.entity.AiModelConfig;
import com.bintech.metrix.repository.entity.User;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.config.TaskManagementConfigUtils;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;
import com.bintech.metrix.constants.BusinessConstants;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Opt-in: exercises the real configured model, market/news sources, database and SSE lifecycle. */
@EnabledIfEnvironmentVariable(named = "METRIX_LIVE_AGENT_TEST", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.import=optional:file:.env[.properties]",
        "python.akshare-script-path=../python-service/akshare.py",
        "financial-data.script-path=../python-service/fuyao.py",
        "market-turnover.baostock-market-turnover-script-path=../python-service/baostock_market_turnover.py",
        "logging.level.com.baomidou.mybatisplus=WARN",
        "logging.level.com.bintech.metrix=INFO"
})
@Import(ChatAgentLiveIntegrationTest.LiveTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatAgentLiveIntegrationTest {
    private static final Duration ANALYSIS_TIMEOUT = Duration.ofMinutes(5);
    private final ChatService chatService;
    private final AiModelService aiModelService;
    private final UserDao userDao;
    private final MarketDataConfigDao marketDataConfigDao;
    private final NewsSourceConfigDao newsSourceConfigDao;
    private final ObjectMapper mapper;
    private final List<Long> createdSessions = new ArrayList<>();
    private MockMvc mvc;
    private Long userId;

    @BeforeAll
    void useExistingConfigurationOwner() {
        User owner = userDao.selectByUsername(System.getenv().getOrDefault("METRIX_LIVE_USERNAME", "admin"));
        assertThat(owner).as("Existing configuration owner must exist").isNotNull();
        userId = owner.getId();
        assertThat(chatService.listSessions(userId).size() + 8).as("Do not evict existing sessions while testing")
                .isLessThan(BusinessConstants.MAX_CHAT_SESSIONS_PER_USER);
        AiModelConfig model = aiModelService.getActiveConfig(userId);
        assertThat(model).isNotNull();
        assertThat(marketDataConfigDao.selectActiveByUserId(userId)).as("Existing active market source").isNotEmpty();
        assertThat(newsSourceConfigDao.selectActiveByUserId(userId)).as("Existing active news source").isNotEmpty();
        System.out.println("LIVE_CONFIG model=" + model.getModelType() + "/" + model.getModelName());
        marketDataConfigDao.selectActiveByUserId(userId).forEach(source -> System.out.println("LIVE_CONFIG market=" + source.getSourceName()));
        newsSourceConfigDao.selectActiveByUserId(userId).forEach(source -> System.out.println("LIVE_CONFIG news=" + source.getSourceName()));
        // Only an in-memory MVC transport: no public test endpoint or authentication changes.
        mvc = MockMvcBuilders.standaloneSetup(new LiveEndpoint(chatService, userId))
                .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        new JacksonJsonHttpMessageConverter((JsonMapper) mapper))
                .setAsyncRequestTimeout(ANALYSIS_TIMEOUT.toMillis()).build();
    }

    @Test
    @Order(1)
    void refusesNonAShareAndBeijingWithoutStartingMarketTools() throws Exception {
        for (String content : List.of("分析美股苹果AAPL", "分析北交所920001", "帮我写一个Python程序")) {
            Long id = createSession();
            MvcResult stream = start(id, content);
            JsonNode done = completed(stream);
            String finalText = frames(wire(stream), "final").getLast().path("content").asText();
            assertThat(finalText).contains("抱歉");
            assertThat(done.path("status").asText()).isEqualTo("refused");
            assertThat(done.path("steps").toString()).doesNotContain("\"stage\":\"tool\"");
            assertThat(chatService.getSessionMessages(id, userId).getLast().getContent()).contains("暂不支持北交所");
        }
        System.out.println("LIVE_PASS refusal: US stock / Beijing / unrelated code task");
    }

    @Test
    @Order(2)
    void asksOnceAndThenRefusesAnUnresolvedTarget() throws Exception {
        Long id = createSession();
        assertThat(completed(start(id, "分析一下那家公司最近的走势")).path("status").asText()).isEqualTo("clarification");
        assertThat(chatService.getSessionMessages(id, userId).getLast().getContent()).isEqualTo("请提供A股名称或6位代码，例如 贵州茅台 600519。");
        assertThat(completed(start(id, "就是那家公司，我也不知道名称")).path("status").asText()).isEqualTo("refused");
        System.out.println("LIVE_PASS clarification: asks exactly once, then refuses");
    }

    @Test
    @Order(3)
    void analyzesAnAShareUsingRealPublicDataAndPersistsTheFinalReply() throws Exception {
        Long id = createSession();
        assertThat(completed(start(id, "分析那家公司最近60个交易日的走势，重点关注技术面、估值和风险"))
                .path("status").asText()).isEqualTo("clarification");
        MvcResult stream = start(id, "贵州茅台600519");
        JsonNode done = completed(stream);
        String answer = chatService.getSessionMessages(id, userId).getLast().getContent();
        Files.writeString(Path.of(System.getProperty("java.io.tmpdir"), "metrix-agent-live-analysis.json"), mapper.writeValueAsString(
                Map.of("done", done, "answer", answer)), StandardCharsets.UTF_8);
        assertThat(done.path("status").asText()).isEqualTo("completed");
        JsonNode steps = done.path("steps");
        assertThat(hasSuccessfulTool(steps, "实时行情")).as("Real market quote must be available").isTrue();
        assertThat(hasSuccessfulTool(steps, "K线")).as("Real Kline data must be available").isTrue();
        assertThat(answer).contains("## 结论摘要", "## 分析依据", "## 风险提示", "不构成投资建议");
        assertThat(answer).containsAnyOf("MA", "MACD", "均线", "市盈率");
        assertThat(answer).doesNotContain("<think>", "【思考过程】");
        assertThat(wire(stream)).contains("event:thinking", "event:final", "event:done");
        System.out.println("LIVE_PASS A-share: real quotes + Klines + model streaming, reply length=" + answer.length());
    }

    @Test
    @Order(4)
    void stopsAnActualModelStreamAndKeepsThePersistedPartialReplyStable() throws Exception {
        Long id = createSession();
        MvcResult stream = start(id, "分析贵州茅台600519的基本面、技术面、资金面、估值和风险，充分说明公开数据依据");
        await().atMost(ANALYSIS_TIMEOUT).pollInterval(Duration.ofMillis(100))
                .until(() -> frames(wire(stream), "final").stream().anyMatch(frame -> frame.path("delta").asBoolean()));
        long stoppedAt = System.nanoTime();
        mvc.perform(post("/live/stop").contentType("application/json").content("{\"sessionId\":" + id + "}"))
                .andExpect(status().isOk());
        JsonNode done = completed(stream);
        assertThat(done.path("status").asText()).isEqualTo("stopped");
        assertThat(Duration.ofNanos(System.nanoTime() - stoppedAt)).isLessThan(Duration.ofSeconds(5));
        List<ChatMessageVO> before = chatService.getSessionMessages(id, userId);
        assertThat(before).hasSize(2);
        String saved = before.getLast().getContent();
        Thread.sleep(3000);
        assertThat(chatService.getSessionMessages(id, userId).getLast().getContent()).isEqualTo(saved);
        System.out.println("LIVE_PASS stop: active model stream stopped, persisted partial reply stable");
    }

    @Test
    @Order(5)
    void stopsDuringModelThinkingBeforeAnyFinalTextIsPublished() throws Exception {
        Long id = createSession();
        MvcResult stream = start(id, "分析贵州茅台600519的技术面、基本面与风险，说明公开数据依据");
        await().atMost(ANALYSIS_TIMEOUT).pollInterval(Duration.ofMillis(100))
                .until(() -> frames(wire(stream), "thinking").stream()
                        .anyMatch(frame -> frame.path("stage").asText().equals("generation")));
        Thread.sleep(2000);
        assertThat(frames(wire(stream), "final")).as("Stop while the reasoning model has not produced final text").isEmpty();
        long stoppedAt = System.nanoTime();
        mvc.perform(post("/live/stop").contentType("application/json").content("{\"sessionId\":" + id + "}"))
                .andExpect(status().isOk());
        assertThat(completed(stream).path("status").asText()).isEqualTo("stopped");
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - stoppedAt).toMillis();
        assertThat(elapsedMillis).isLessThan(5000L);
        String saved = chatService.getSessionMessages(id, userId).getLast().getContent();
        assertThat(saved).contains("不构成投资建议").doesNotContain("<think>", "【思考过程】");
        Thread.sleep(3000);
        assertThat(chatService.getSessionMessages(id, userId).getLast().getContent()).isEqualTo(saved);
        System.out.println("LIVE_PASS thinking stop: stopped before final output, terminal response in " + elapsedMillis + "ms");
    }

    private Long createSession() {
        ChatSessionVO session = chatService.createSession(userId, "Agent真实联调 · 临时验证");
        createdSessions.add(session.getId());
        return session.getId();
    }

    private MvcResult start(Long id, String content) throws Exception {
        return mvc.perform(post("/live/send").contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("sessionId", id, "content", content))))
                .andExpect(request().asyncStarted()).andReturn();
    }

    private JsonNode completed(MvcResult stream) throws Exception {
        await().atMost(ANALYSIS_TIMEOUT).pollInterval(Duration.ofMillis(100))
                .until(() -> !frames(wire(stream), "done").isEmpty());
        mvc.perform(asyncDispatch(stream)).andExpect(status().isOk());
        return frames(wire(stream), "done").getLast();
    }

    private String wire(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private List<JsonNode> frames(String wire, String event) {
        List<JsonNode> data = new ArrayList<>();
        String[] completeFrames = wire.split("\\r?\\n\\r?\\n", -1);
        for (int index = 0; index < completeFrames.length - 1; index++) {
            String frame = completeFrames[index];
            if (!frame.startsWith("event:" + event)) continue;
            String payload = frame.lines().filter(line -> line.startsWith("data:"))
                    .map(line -> line.substring(5).stripLeading()).reduce((a, b) -> a + "\n" + b).orElse("");
            data.add(mapper.readTree(payload));
        }
        return data;
    }

    private boolean hasSuccessfulTool(JsonNode steps, String title) {
        for (JsonNode step : steps) {
            if (step.path("content").asText().contains(title) && step.path("status").asText().equals("completed")) return true;
        }
        return false;
    }

    @AfterAll
    void deleteOnlyTheSessionsCreatedByThisTest() {
        if (userId == null) return;
        createdSessions.forEach(id -> { chatService.stopGeneration(id, userId); chatService.deleteSession(id, userId); });
    }

    @RestController
    @RequiredArgsConstructor
    static class LiveEndpoint {
        private final ChatService service;
        private final Long ownerId;
        @PostMapping("/live/send")
        SseEmitter send(@Valid @RequestBody ChatSendRequest request) {
            return service.sendMessage(request.getSessionId(), ownerId, request.getContent());
        }
        @PostMapping("/live/stop")
        void stop(@RequestBody Map<String, Long> request) {
            service.stopGeneration(request.get("sessionId"), ownerId);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LiveTestConfiguration {
        @Bean
        static BeanFactoryPostProcessor disableUnrelatedScheduledWork() {
            return factory -> {
                BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
                String name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME;
                if (registry.containsBeanDefinition(name)) registry.removeBeanDefinition(name);
            };
        }
    }
}
