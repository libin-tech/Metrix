package com.bintech.metrix.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 止盈计划相对当前行情的提醒状态。 */
@Getter
@AllArgsConstructor
public enum TakeProfitPlanStatus {
    DISABLED("未启用"),
    WAITING("等待行情"),
    FIRST_TARGET_REACHED("第一目标已触达"),
    SECOND_TARGET_REACHED("第二目标已触达"),
    TRACKING("跟踪止盈中"),
    TRAILING_STOP_TRIGGERED("跟踪止盈已触发"),
    EXPIRED("已到期");

    private final String description;
}
