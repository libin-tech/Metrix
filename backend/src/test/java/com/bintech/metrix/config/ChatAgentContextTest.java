package com.bintech.metrix.config;

import com.bintech.metrix.core.analysis.ChatAgentIntentResolver;
import com.bintech.metrix.core.analysis.MarketReviewDataFetcher;
import com.bintech.metrix.core.analysis.MarketReviewPromptBuilder;
import com.bintech.metrix.core.analysis.StockAdvisorPromptBuilder;
import com.bintech.metrix.repository.dao.ChatMessageDao;
import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.ChatService;
import com.bintech.metrix.service.ChatSessionService;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.NewsService;
import com.bintech.metrix.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ChatAgentContextTest {
    @Test
    void agentStartsWithTheActualSpringBootJacksonAutoConfiguration() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(AgentComponents.class)
                .withBean(ChatSessionService.class, () -> mock(ChatSessionService.class))
                .withBean(ChatMessageDao.class, () -> mock(ChatMessageDao.class))
                .withBean(StockBasicDao.class, () -> mock(StockBasicDao.class))
                .withBean(AiModelService.class, () -> mock(AiModelService.class))
                .withBean(StockAdvisorPromptBuilder.class, StockAdvisorPromptBuilder::new)
                .withBean(MarketReviewPromptBuilder.class, MarketReviewPromptBuilder::new)
                .withBean(MarketReviewDataFetcher.class, () -> mock(MarketReviewDataFetcher.class))
                .withBean(MarketDataService.class, () -> mock(MarketDataService.class))
                .withBean(NewsService.class, () -> mock(NewsService.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ChatService.class);
                    assertThat(context).hasSingleBean(ChatAgentIntentResolver.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({ChatServiceImpl.class, ChatAgentIntentResolver.class})
    static class AgentComponents {}
}
