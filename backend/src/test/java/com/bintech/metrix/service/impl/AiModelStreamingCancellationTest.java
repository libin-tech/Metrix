package com.bintech.metrix.service.impl;

import com.bintech.metrix.repository.dao.AiModelConfigDao;
import com.bintech.metrix.repository.entity.AiModelConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiModelStreamingCancellationTest {
    @Test
    void cancellationBeforeSubmissionDoesNotOpenAModelRequest() {
        AiModelConfigDao dao = mock(AiModelConfigDao.class);
        AiModelServiceImpl service = new AiModelServiceImpl(dao);
        assertDoesNotThrow(() -> service.generateAnalysisStreaming("test", "OPENAI", 1L,
                token -> {}, result -> {}, error -> {}, () -> true));
        verifyNoInteractions(dao);
    }

    @Test
    void cancellationDuringPrivateReasoningClosesTheUnderlyingConnectionWithoutPublishingThoughts() throws Exception {
        CountDownLatch reasoningStarted = new CountDownLatch(1);
        CountDownLatch disconnected = new CountDownLatch(1);
        AtomicBoolean cancelled = new AtomicBoolean();
        List<String> visibleTokens = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(task -> Thread.ofVirtual().start(task));
        server.createContext("/v1/chat/completions", exchange -> sendPrivateReasoning(exchange, reasoningStarted, disconnected));
        server.start();
        try {
            AiModelConfig config = new AiModelConfig();
            config.setModelName("fixture-reasoner");
            config.setApiBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            config.setApiKey("local-fixture-key");
            config.setTemperature(0.0);
            config.setTimeout(10);
            AiModelConfigDao dao = mock(AiModelConfigDao.class);
            when(dao.selectOneActiveByUserIdAndModelType(1L, "OPENAI")).thenReturn(config);
            new AiModelServiceImpl(dao).generateAnalysisStreaming("test", "OPENAI", 1L,
                    visibleTokens::add, result -> {}, error -> {}, cancelled::get);
            assertTrue(reasoningStarted.await(3, TimeUnit.SECONDS));
            cancelled.set(true);
            assertTrue(disconnected.await(3, TimeUnit.SECONDS), "Stopping during reasoning must close the model HTTP stream, without waiting for final text");
            assertTrue(visibleTokens.isEmpty(), "Private reasoning must never be forwarded to answer tokens");
        } finally {
            cancelled.set(true);
            server.stop(0);
        }
    }

    private void sendPrivateReasoning(HttpExchange exchange, CountDownLatch started, CountDownLatch disconnected) throws IOException {
        exchange.getRequestBody().readAllBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        exchange.sendResponseHeaders(200, 0);
        String payload = "data: {\"id\":\"fixture\",\"object\":\"chat.completion.chunk\",\"model\":\"fixture-reasoner\","
                + "\"choices\":[{\"index\":0,\"delta\":{\"reasoning_content\":\"" + "private-fixture ".repeat(300)
                + "\"},\"finish_reason\":null}]}\n\n";
        try (OutputStream output = exchange.getResponseBody()) {
            for (int index = 0; index < 60; index++) {
                output.write(payload.getBytes(StandardCharsets.UTF_8));
                output.flush();
                started.countDown();
                Thread.sleep(100);
            }
        } catch (IOException error) {
            disconnected.countDown();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } finally {
            exchange.close();
        }
    }
}
