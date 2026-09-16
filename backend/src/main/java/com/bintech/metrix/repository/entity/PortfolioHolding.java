package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 持仓标的实体
 *
 * <p>对应 portfolio_holding 表，记录每个券商账户下的持仓标的。
 * 同一账户下不允许重复添加同一标的。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("portfolio_holding")
public class PortfolioHolding extends BaseEntity {

    /** 券商账户ID（关联 broker_account.id） */
    @TableField(value = "account_id")
    private Long accountId;

    /** 标的代码（如 000001.SZ） */
    @TableField(value = "stock_code")
    private String stockCode;

    /** 标的名称 */
    @TableField(value = "stock_name")
    private String stockName;

    /** 持仓成本价（保留3位小数） */
    @TableField(value = "cost")
    private BigDecimal cost;

    /** 持有数量 */
    @TableField(value = "quantity")
    private BigDecimal quantity;

    /** 用户ID（数据隔离） */
    @TableField(value = "user_id")
    private Long userId;

    /** 缓存的最新行情价格 */
    @TableField(value = "cached_price")
    private BigDecimal cachedPrice;

    /** 行情缓存时间 */
    @TableField(value = "cached_price_time")
    private LocalDateTime cachedPriceTime;

    /** 是否启用止盈计划 */
    @TableField(value = "take_profit_enabled")
    private Boolean takeProfitEnabled;

    /** 第一目标价 */
    @TableField(value = "take_profit_first_target_price")
    private BigDecimal takeProfitFirstTargetPrice;

    /** 第一档减仓比例 */
    @TableField(value = "take_profit_first_target_ratio")
    private BigDecimal takeProfitFirstTargetRatio;

    /** 第二目标价 */
    @TableField(value = "take_profit_second_target_price")
    private BigDecimal takeProfitSecondTargetPrice;

    /** 第二档减仓比例 */
    @TableField(value = "take_profit_second_target_ratio")
    private BigDecimal takeProfitSecondTargetRatio;

    /** 剩余仓位从阶段高点回撤的止盈比例 */
    @TableField(value = "take_profit_trailing_drawdown_percent")
    private BigDecimal takeProfitTrailingDrawdownPercent;

    /** 跟踪止盈阶段最高价 */
    @TableField(value = "take_profit_trailing_peak_price")
    private BigDecimal takeProfitTrailingPeakPrice;

    /** 是否已触发跟踪止盈提醒 */
    @TableField(value = "take_profit_trailing_triggered")
    private Boolean takeProfitTrailingTriggered;

    /** 止盈计划有效期 */
    @TableField(value = "take_profit_expires_on")
    private LocalDate takeProfitExpiresOn;
}
