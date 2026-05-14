package com.bintech.metrix.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 股票实时行情模块
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealTimeMarket {

    /**
     * 当前涨跌幅百分比（带正负号）
     */
    private BigDecimal changePercent;

    /**
     * 当前价格
     */
    private BigDecimal currentPrice;

    /**
     * 涨跌金额
     */
    private BigDecimal changeAmount;

    /**
     * 开盘价
     */
    private BigDecimal openPrice;

    /**
     * 最高价
     */
    private BigDecimal highPrice;

    /**
     * 最低价
     */
    private BigDecimal lowPrice;

    /**
     * 昨收价
     */
    private BigDecimal prevClosePrice;

    /**
     * 成交量（股）
     */
    private Long volume;

    /**
     * 成交额（元）
     */
    private BigDecimal turnover;

    /**
     * 换手率
     */
    private BigDecimal turnoverRate;

    /**
     * 市盈率（TTM）
     */
    private BigDecimal peTtm;

    /**
     * 市净率
     */
    private BigDecimal pb;

    /**
     * 振幅
     */
    private BigDecimal amplitude;

    /**
     * 涨停价
     */
    private BigDecimal upLimitPrice;

    /**
     * 跌停价
     */
    private BigDecimal downLimitPrice;

    /**
     * 更新时间戳
     */
    private String updateTime;
}