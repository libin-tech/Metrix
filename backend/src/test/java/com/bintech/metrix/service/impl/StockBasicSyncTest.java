package com.bintech.metrix.service.impl;

import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.MarketDataService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockBasicSyncTest {
    private StockBasic row(String code, String name) {
        StockBasic row = new StockBasic();
        row.setTsCode(code);
        row.setSymbol(code.substring(0, 6));
        row.setName(name);
        return row;
    }

    @Test
    void shouldGuideWithoutConfigAndNeverFetchOrWrite() {
        StockBasicDao dao = mock(StockBasicDao.class);
        MarketDataService marketData = mock(MarketDataService.class);
        StockBasicSyncWriter writer = mock(StockBasicSyncWriter.class);
        var service = new StockBasicServiceImpl(dao, marketData, writer);
        assertTrue(service.sync(1L).configurationRequired());
        verify(marketData, never()).fetchTickerData(any());
        verifyNoInteractions(dao, writer);
    }

    @Test
    void shouldNotWriteAfterRemoteFailure() {
        StockBasicDao dao = mock(StockBasicDao.class);
        MarketDataService marketData = mock(MarketDataService.class);
        StockBasicSyncWriter writer = mock(StockBasicSyncWriter.class);
        when(marketData.hasActiveConfig(1L)).thenReturn(true);
        when(marketData.fetchTickerData(1L)).thenThrow(new IllegalStateException("upstream"));
        var service = new StockBasicServiceImpl(dao, marketData, writer);
        assertThrows(IllegalStateException.class, () -> service.sync(1L));
        verifyNoInteractions(dao, writer);
    }

    @Test
    void shouldInsertChangedAndSkipUnchangedWithoutClearingOtherFields() {
        StockBasicDao dao = mock(StockBasicDao.class);
        StockBasic existing = row("600000.SH", "旧名称");
        existing.setId(1L);
        existing.setVersion(4);
        existing.setIndustry("银行");
        StockBasic unchanged = row("000001.SZ", "平安银行");
        when(dao.selectByTsCodeIn(anyList())).thenReturn(List.of(existing, unchanged));
        var writer = new StockBasicSyncWriter(dao);
        var result = writer.apply(List.of(row("600000.SH", "新名称"), row("000001.SZ", "平安银行"),
                row("920001.BJ", "新股"), row("920001.BJ", "新股")));
        assertEquals(1, result.inserted());
        assertEquals(1, result.updated());
        assertEquals(1, result.unchanged());
        assertEquals(3, result.total());
        ArgumentCaptor<List<StockBasic>> updates = ArgumentCaptor.captor();
        verify(dao).updateBatch(updates.capture());
        StockBasic patch = updates.getValue().getFirst();
        assertEquals(1L, patch.getId());
        assertEquals(4, patch.getVersion());
        assertEquals("新名称", patch.getName());
        assertNull(patch.getIndustry());
        assertNull(patch.getListDate());
        assertEquals("银行", existing.getIndustry());
        verify(dao, never()).deleteById(anyLong());
    }

    @Test
    void shouldSkipEveryRecordWhenSnapshotIsUnchanged() {
        StockBasicDao dao = mock(StockBasicDao.class);
        var rows = List.of(row("600000.SH", "浦发银行"));
        when(dao.selectByTsCodeIn(anyList())).thenReturn(rows);
        var result = new StockBasicSyncWriter(dao).apply(rows);
        assertEquals(0, result.inserted());
        assertEquals(0, result.updated());
        assertEquals(1, result.unchanged());
        verify(dao).insertBatch(List.of());
        verify(dao).updateBatch(List.of());
    }

    @Test
    void shouldValidateWholeSnapshotBeforeDatabaseAccess() {
        StockBasicDao dao = mock(StockBasicDao.class);
        var writer = new StockBasicSyncWriter(dao);
        assertThrows(IllegalArgumentException.class, () -> writer.apply(List.of()));
        assertThrows(IllegalArgumentException.class, () -> writer.apply(List.of(row("600000.SH", "浦发银行"), row("000001.SZ", ""))));
        verifyNoInteractions(dao);
    }

    @Test
    void shouldPassCompleteTickerResponseToWriter() {
        StockBasicDao dao = mock(StockBasicDao.class);
        MarketDataService marketData = mock(MarketDataService.class);
        StockBasicSyncWriter writer = mock(StockBasicSyncWriter.class);
        when(marketData.hasActiveConfig(1L)).thenReturn(true);
        when(marketData.fetchTickerData(1L)).thenReturn(Map.of("status", "success", "data",
                List.of(Map.of("tsCode", "600000.SH", "symbol", "600000", "name", "浦发银行"))));
        new StockBasicServiceImpl(dao, marketData, writer).sync(1L);
        ArgumentCaptor<List<StockBasic>> rows = ArgumentCaptor.captor();
        verify(writer).apply(rows.capture());
        assertEquals("600000.SH", rows.getValue().getFirst().getTsCode());
    }
}
