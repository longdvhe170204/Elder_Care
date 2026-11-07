package com.eldercare.backend.resident.internal.status.domain;

import java.time.Instant;

/** Một dòng lịch sử trạng thái hoặc mức chăm sóc cho số liệu của 016: {@code value} là trạng thái hoặc mã mức. */
public record HistoryPoint(long residentId, Instant at, String command, String valueBefore, String value) {}
