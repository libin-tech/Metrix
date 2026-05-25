package com.bintech.metrix.core.queue;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 大盘复盘任务体
 */
@Data
@AllArgsConstructor
public class MarketReviewTask {
    private Long reviewId;
    private String reviewDate;
}
