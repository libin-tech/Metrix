package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bintech.metrix.enums.MarketReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 大盘复盘记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("market_review")
public class MarketReview extends BaseEntity {

    /** 复盘日期，格式：2026-05-19 */
    @TableField(value = "review_date")
    private String reviewDate;

    /** 复盘名称，格式：2026-05-19A股复盘报告 */
    @TableField(value = "review_name")
    private String reviewName;

    /** 复盘时间 */
    @TableField(value = "review_time")
    private LocalDateTime reviewTime;

    /** 复盘状态：REVIEWING-复盘中，COMPLETED-复盘完成，FAILED-复盘失败 */
    @TableField(value = "status")
    private MarketReviewStatus status;

    /** 复盘详情（Markdown格式） */
    @TableField(value = "detail")
    private String detail;

    /** 总结 */
    @TableField(value = "summary")
    private String summary;

    /** 核心总结（500字以内） */
    @TableField(value = "core_summary")
    private String coreSummary;

    /** 错误信息 */
    @TableField(value = "error_message")
    private String errorMessage;
}
