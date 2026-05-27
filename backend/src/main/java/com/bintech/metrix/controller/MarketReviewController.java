package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.CursorPageResult;
import com.bintech.metrix.enums.MarketReviewStatus;
import com.bintech.metrix.enums.UserRole;
import com.bintech.metrix.repository.entity.MarketReview;
import com.bintech.metrix.repository.entity.User;
import com.bintech.metrix.service.MarketReviewService;
import com.bintech.metrix.service.UsageStatsService;
import com.bintech.metrix.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 大盘复盘控制器
 */
@RestController
@RequestMapping("/api/market-review")
@RequiredArgsConstructor
@SaCheckLogin
public class MarketReviewController {

    private final MarketReviewService marketReviewService;
    private final UsageStatsService usageStatsService;
    private final UserService userService;

    @GetMapping
    @SaCheckPermission("review:record:list")
    public ApiResponse<List<MarketReview>> getAllReviews() {
        List<MarketReview> reviews = marketReviewService.getAllReviews();
        return ApiResponse.success(reviews);
    }

    @GetMapping("/cursor")
    @SaCheckPermission("review:record:cursor")
    public ApiResponse<CursorPageResult<MarketReview>> cursorQuery(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int limit) {
        CursorPageResult<MarketReview> result = marketReviewService.cursorQuery(cursor, limit);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    @SaCheckPermission("review:record:detail")
    public ApiResponse<MarketReview> getReviewById(@PathVariable Long id) {
        MarketReview review = marketReviewService.getReviewById(id);
        return ApiResponse.success(review);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("review:record:delete")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        marketReviewService.deleteReview(id);
        return ApiResponse.success("复盘记录删除成功", null);
    }

    @PostMapping("/trigger")
    @SaCheckPermission("review:trigger")
    public ApiResponse<Map<String, Object>> triggerReview() {
        Map<String, Object> result = marketReviewService.triggerReview();
        return ApiResponse.success(result);
    }

    @PostMapping("/create")
    @SaCheckPermission("review:record:create")
    public ApiResponse<MarketReview> createReview(@RequestParam String reviewDate) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getUserById(userId);
        boolean isAdmin = UserRole.ADMIN == user.getRole();
        if (!isAdmin && !usageStatsService.checkAndIncrementReview(userId)) {
            return ApiResponse.error("每日大盘复盘次数限制（5次）已用完，请明天再试");
        }
        MarketReview review = marketReviewService.createReview(reviewDate);
        return ApiResponse.success("大盘复盘任务已创建", review);
    }
}
