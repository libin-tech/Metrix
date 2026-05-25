package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 券商账户实体
 *
 * <p>对应 broker_account 表，记录用户管理的券商账户信息。
 * 一个账户下可关联多个持仓标的。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("broker_account")
public class BrokerAccount extends BaseEntity {

    /** 券商名称 */
    @TableField(value = "broker_name")
    private String brokerName;

    /** 账号（展示时仅显示后四位） */
    @TableField(value = "account_number")
    private String accountNumber;

    /** 备注 */
    @TableField(value = "remark")
    private String remark;
}
