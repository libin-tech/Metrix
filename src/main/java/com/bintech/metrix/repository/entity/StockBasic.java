package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("stock_basic")
public class StockBasic extends BaseEntity {

    @TableField(value = "ts_code")
    private String tsCode;

    @TableField(value = "symbol")
    private String symbol;

    @TableField(value = "name")
    private String name;

    @TableField(value = "area")
    private String area;

    @TableField(value = "industry")
    private String industry;

    @TableField(value = "cnspell")
    private String cnspell;

    @TableField(value = "market")
    private String market;

    @TableField(value = "list_date")
    private LocalDate listDate;

    @TableField(value = "act_name")
    private String actName;

    @TableField(value = "act_ent_type")
    private String actEntType;
}
