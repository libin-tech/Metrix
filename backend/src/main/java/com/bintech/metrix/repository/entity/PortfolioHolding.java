package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
}
