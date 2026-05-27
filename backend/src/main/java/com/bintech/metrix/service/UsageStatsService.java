package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.UsageStatsVO;

import java.time.LocalDate;
import java.util.List;

public interface UsageStatsService {

    boolean checkAndIncrementAnalysis(Long userId);

    boolean checkAndIncrementReview(Long userId);

    int getDailyAnalysisCount(Long userId);

    int getDailyReviewCount(Long userId);

    List<UsageStatsVO> getAllUsersTodayStats();

    List<UsageStatsVO> getStatsByDateRange(LocalDate startDate, LocalDate endDate);
}
