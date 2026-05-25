package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 股票基础信息实体
 *
 * <p>对应 stock_basic 表，记录股票基础信息，用于行情查询和分析。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("stock_basic")
public class StockBasic extends BaseEntity {

    /** TS代码（如 000001.SZ） */
    @TableField(value = "ts_code")
    private String tsCode;

    /** 股票代码 */
    @TableField(value = "symbol")
    private String symbol;

    /** 股票名称 */
    @TableField(value = "name")
    private String name;

    /** 上市地区 */
    @TableField(value = "area")
    private String area;

    /** 所属行业 */
    @TableField(value = "industry")
    private String industry;

    /** 拼音缩写 */
    @TableField(value = "cnspell")
    private String cnspell;

    /** 市场类型 */
    @TableField(value = "market")
    private String market;

    /** 上市日期 */
    @TableField(value = "list_date")
    private LocalDate listDate;

    /** 实际控制人名称 */
    @TableField(value = "act_name")
    private String actName;

    /** 实际控制人企业性质 */
    @TableField(value = "act_ent_type")
    private String actEntType;
}
