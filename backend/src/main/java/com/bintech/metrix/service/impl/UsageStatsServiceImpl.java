package com.bintech.metrix.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.dto.response.UsageStatsVO;
import com.bintech.metrix.repository.entity.UsageStats;
import com.bintech.metrix.repository.entity.User;
import com.bintech.metrix.repository.mapper.UsageStatsMapper;
import com.bintech.metrix.repository.mapper.UserMapper;
import com.bintech.metrix.service.UsageStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageStatsServiceImpl implements UsageStatsService {

    private final UsageStatsMapper usageStatsMapper;
    private final UserMapper userMapper;

    private static final int DAILY_ANALYSIS_LIMIT = 2;
    private static final int DAILY_REVIEW_LIMIT = 5;

    @Override
    @Transactional
    public boolean checkAndIncrementAnalysis(Long userId) {
        LocalDate today = LocalDate.now();
        UsageStats stats = getOrCreateStats(userId, today);
        if (stats.getAnalysisCount() >= DAILY_ANALYSIS_LIMIT) {
            return false;
        }
        stats.setAnalysisCount(stats.getAnalysisCount() + 1);
        usageStatsMapper.updateById(stats);
        return true;
    }

    @Override
    @Transactional
    public boolean checkAndIncrementReview(Long userId) {
        LocalDate today = LocalDate.now();
        UsageStats stats = getOrCreateStats(userId, today);
        if (stats.getReviewCount() >= DAILY_REVIEW_LIMIT) {
            return false;
        }
        stats.setReviewCount(stats.getReviewCount() + 1);
        usageStatsMapper.updateById(stats);
        return true;
    }

    @Override
    public int getDailyAnalysisCount(Long userId) {
        LocalDate today = LocalDate.now();
        UsageStats stats = getOrCreateStats(userId, today);
        return stats.getAnalysisCount();
    }

    @Override
    public int getDailyReviewCount(Long userId) {
        LocalDate today = LocalDate.now();
        UsageStats stats = getOrCreateStats(userId, today);
        return stats.getReviewCount();
    }

    @Override
    public List<UsageStatsVO> getAllUsersTodayStats() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<UsageStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageStats::getStatDate, today);
        List<UsageStats> statsList = usageStatsMapper.selectList(wrapper);

        if (statsList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = statsList.stream()
                .map(UsageStats::getUserId)
                .collect(Collectors.toList());
        Map<Long, String> usernameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getUsername() != null ? u.getUsername() : ""));
        Map<Long, String> nicknameMap = userMapper.selectBatchIds(userIds).stream()
                .filter(u -> u.getNickname() != null)
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return statsList.stream().map(s -> UsageStatsVO.builder()
                .userId(s.getUserId())
                .username(usernameMap.getOrDefault(s.getUserId(), ""))
                .nickname(nicknameMap.get(s.getUserId()))
                .statDate(s.getStatDate())
                .analysisCount(s.getAnalysisCount())
                .reviewCount(s.getReviewCount())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<UsageStatsVO> getStatsByDateRange(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<UsageStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(UsageStats::getStatDate, startDate, endDate)
                .orderByAsc(UsageStats::getStatDate);
        List<UsageStats> statsList = usageStatsMapper.selectList(wrapper);

        List<Long> userIds = statsList.stream()
                .map(UsageStats::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> usernameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getUsername() != null ? u.getUsername() : ""));

        return statsList.stream().map(s -> UsageStatsVO.builder()
                .userId(s.getUserId())
                .username(usernameMap.getOrDefault(s.getUserId(), ""))
                .statDate(s.getStatDate())
                .analysisCount(s.getAnalysisCount())
                .reviewCount(s.getReviewCount())
                .build()).collect(Collectors.toList());
    }

    private UsageStats getOrCreateStats(Long userId, LocalDate date) {
        LambdaQueryWrapper<UsageStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageStats::getUserId, userId)
                .eq(UsageStats::getStatDate, date);
        UsageStats stats = usageStatsMapper.selectOne(wrapper);
        if (stats == null) {
            stats = new UsageStats();
            stats.setUserId(userId);
            stats.setStatDate(date);
            stats.setAnalysisCount(0);
            stats.setReviewCount(0);
            usageStatsMapper.insert(stats);
        }
        return stats;
    }
}
