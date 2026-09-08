package com.bintech.metrix.service.impl;

import com.bintech.metrix.repository.dao.AiModelConfigDao;
import com.bintech.metrix.repository.entity.AiModelConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiModelServiceImplTest {

    @Mock
    private AiModelConfigDao configDao;

    @Test
    void shouldUseMostRecentlyUpdatedActiveModelType() {
        AiModelConfig openAi = activeConfig("OPENAI", LocalDateTime.of(2026, 9, 6, 9, 0));
        AiModelConfig ollama = activeConfig("OLLAMA", LocalDateTime.of(2026, 9, 7, 9, 0));
        when(configDao.selectActiveByUserId(1L)).thenReturn(List.of(openAi, ollama));

        String result = service().getActiveModelType(1L);

        assertEquals("OLLAMA", result);
        verify(configDao).selectActiveByUserId(1L);
    }

    @Test
    void shouldUseOpenAiWhenNoActiveModelExists() {
        when(configDao.selectActiveByUserId(1L)).thenReturn(List.of());

        assertEquals("OPENAI", service().getActiveModelType(1L));
    }

    @Test
    void shouldUseOpenAiWhenActiveModelHasNoType() {
        when(configDao.selectActiveByUserId(1L)).thenReturn(List.of(activeConfig(null, LocalDateTime.now())));

        assertEquals("OPENAI", service().getActiveModelType(1L));
    }

    @Test
    void shouldUseOpenAiWithoutAUserContext() {
        assertEquals("OPENAI", service().getActiveModelType(null));
        verifyNoInteractions(configDao);
    }

    private AiModelServiceImpl service() {
        return new AiModelServiceImpl(configDao);
    }

    private AiModelConfig activeConfig(String modelType, LocalDateTime updateTime) {
        AiModelConfig config = new AiModelConfig();
        config.setModelType(modelType);
        config.setUpdateTime(updateTime);
        config.setIsActive(true);
        return config;
    }
}
