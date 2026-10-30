package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.DailyMarketIntelligence.Result;

public interface DailyMarketIntelligenceService {
    Result getReport(Long userId);
    void refreshMarketHistory();
}
