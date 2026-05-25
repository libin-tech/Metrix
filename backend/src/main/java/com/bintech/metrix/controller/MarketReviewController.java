package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.CursorPageResult;
import com.bintech.metrix.repository.entity.MarketReview;
import com.bintech.metrix.service.MarketReviewService;
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

    @GetMapping
    public ApiResponse<List<MarketReview>> getAllReviews() {
        List<MarketReview> reviews = marketReviewService.getAllReviews();
        return ApiResponse.success(reviews);
    }

    @GetMapping("/cursor")
    public ApiResponse<CursorPageResult<MarketReview>> cursorQuery(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int limit) {
        CursorPageResult<MarketReview> result = marketReviewService.cursorQuery(cursor, limit);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<MarketReview> getReviewById(@PathVariable Long id) {
        MarketReview review = marketReviewService.getReviewById(id);
        return ApiResponse.success(review);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        marketReviewService.deleteReview(id);
        return ApiResponse.success("复盘记录删除成功", null);
    }

    @PostMapping("/trigger")
    public ApiResponse<Map<String, Object>> triggerReview() {
        Map<String, Object> result = marketReviewService.triggerReview();
        return ApiResponse.success(result);
    }

    @PostMapping("/create")
    public ApiResponse<MarketReview> createReview(@RequestParam String reviewDate) {
        MarketReview review = marketReviewService.createReview(reviewDate);
        return ApiResponse.success("大盘复盘任务已创建", review);
    }
}
