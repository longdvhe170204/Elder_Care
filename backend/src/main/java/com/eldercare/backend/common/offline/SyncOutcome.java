package com.eldercare.backend.common.offline;

/** Kết quả đồng bộ kèm lý do (lý do rỗng khi ACCEPTED). */
public record SyncOutcome(SyncResult result, String reason) {}
