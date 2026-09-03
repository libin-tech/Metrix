package com.bintech.metrix.service.impl;

import com.bintech.metrix.core.analysis.MarketReviewDataFetcher;
import com.bintech.metrix.core.analysis.MarketReviewPromptBuilder;
import com.bintech.metrix.enums.MarketReviewStatus;
import com.bintech.metrix.repository.dao.MarketReviewDao;
import com.bintech.metrix.repository.entity.MarketReview;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketReviewServiceImplTest {

    @Mock
    private MarketReviewDao marketReviewDao;
    @Mock
    private AiModelService aiModelService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MarketReviewDataFetcher marketReviewDataFetcher;
    @Mock
    private MarketReviewPromptBuilder marketReviewPromptBuilder;

    @Test
    void shouldPersistPromptBeforeSubmittingAiAnalysis() {
        MarketReview review = new MarketReview();
        review.setId(1L);
        Map<String, Object> indexData = Map.of("上证指数", Map.of("changePct", 1.0));
        Map<String, Object> marketTurnoverData = Map.of("history", List.of(
                Map.of("date", "2026-08-31", "volume", 10L, "amount", 100L)));
        MarketReviewServiceImpl service = new MarketReviewServiceImpl(
                marketReviewDao, aiModelService, notificationService, marketReviewDataFetcher, marketReviewPromptBuilder);

        when(marketReviewDataFetcher.fetchIndexData("2026-08-31")).thenReturn(indexData);
        when(marketReviewDataFetcher.fetchMarketTurnoverData("2026-08-31")).thenReturn(marketTurnoverData);
        when(marketReviewPromptBuilder.build(indexData, marketTurnoverData, "2026-08-31")).thenReturn("完整AI提示词");
        when(marketReviewDao.selectById(1L)).thenReturn(review);
        when(aiModelService.getActiveModelType(2L)).thenReturn("openai");
        when(aiModelService.generateAnalysis("完整AI提示词", "openai", 2L)).thenReturn("分析结果");

        service.processReview(1L, "2026-08-31", 2L);

        InOrder invocationOrder = inOrder(marketReviewDao, aiModelService);
        invocationOrder.verify(marketReviewDao).updateById(review);
        invocationOrder.verify(aiModelService).getActiveModelType(2L);
        verify(aiModelService).generateAnalysis("完整AI提示词", "openai", 2L);
        verify(marketReviewDao, times(2)).updateById(review);
        assertEquals("完整AI提示词", review.getPrompt());
        assertEquals(MarketReviewStatus.COMPLETED, review.getStatus());
    }
}
