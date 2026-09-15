package com.bintech.metrix.service;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketTradingSessionTest {

    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");

    @Test
    void shouldOnlyTreatContinuousAuctionHoursOnWeekdaysAsTrading() {
        MarketTradingSession session = new MarketTradingSession();

        assertTrue(session.isTradingAt(time(2026, 9, 14, 9, 30)));
        assertTrue(session.isTradingAt(time(2026, 9, 14, 14, 59)));
        assertFalse(session.isTradingAt(time(2026, 9, 14, 9, 29)));
        assertFalse(session.isTradingAt(time(2026, 9, 14, 11, 30)));
        assertFalse(session.isTradingAt(time(2026, 9, 14, 12, 0)));
        assertFalse(session.isTradingAt(time(2026, 9, 14, 15, 0)));
        assertFalse(session.isTradingAt(time(2026, 9, 12, 10, 0)));
    }

    private ZonedDateTime time(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, MARKET_ZONE);
    }
}
