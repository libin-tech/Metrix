package com.bintech.metrix.service;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** 判断 A 股连续竞价交易时段。 */
@Component
public class MarketTradingSession {

    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalTime MORNING_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MORNING_CLOSE = LocalTime.of(11, 30);
    private static final LocalTime AFTERNOON_OPEN = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_CLOSE = LocalTime.of(15, 0);

    /** 当前时间是否处于工作日的 A 股盘中时段。 */
    public boolean isTrading() {
        return isTradingAt(ZonedDateTime.now(MARKET_ZONE));
    }

    boolean isTradingAt(ZonedDateTime time) {
        if (time.getDayOfWeek() == DayOfWeek.SATURDAY || time.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime localTime = time.toLocalTime();
        return isWithin(localTime, MORNING_OPEN, MORNING_CLOSE) || isWithin(localTime, AFTERNOON_OPEN, AFTERNOON_CLOSE);
    }

    private boolean isWithin(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && time.isBefore(end);
    }
}
