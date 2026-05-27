package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 使用统计实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("usage_stats")
public class UsageStats extends BaseEntity {

    /** 用户ID */
    @TableField(value = "user_id")
    private Long userId;

    /** 统计日期 */
    @TableField(value = "stat_date")
    private LocalDate statDate;

    /** 分析次数 */
    @TableField(value = "analysis_count")
    private Integer analysisCount;

    /** 复盘次数 */
    @TableField(value = "review_count")
    private Integer reviewCount;

}
