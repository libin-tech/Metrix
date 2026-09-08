package com.bintech.metrix.constants;

/** 标的同步校验及数据库批次大小。 */
public final class StockSyncConstants {
    public static final int BATCH_SIZE = 1000;
    public static final int SYMBOL_LENGTH = 6;
    public static final String CODE_PATTERN = "[0-9]{6}\\.(SH|SZ|BJ)";

    private StockSyncConstants() {}
}
