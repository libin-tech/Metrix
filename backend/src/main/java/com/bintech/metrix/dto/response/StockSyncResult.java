package com.bintech.metrix.dto.response;

/** 标的增量同步结果，缺少配置时不访问上游或写库。 */
public record StockSyncResult(boolean configurationRequired, int inserted, int updated, int unchanged, int total) {
}
