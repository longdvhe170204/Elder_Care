package com.eldercare.backend.incident.internal.emergency.domain;

import java.time.Instant;

/**
 * Lượt bấm "Gọi 115" ({@code luot_goi_115}, data-model mục 7; R-15: {@code record_event} kind {@code M05.CALL_115}, cha
 * là cảnh báo hoặc sự cố). Kết quả đặt một lần ở dòng con {@code M05.CALL_115_RESULT}.
 */
public record Call115Content(Long alertId, Long incidentId, long pressedBy, Instant pressedAt) {}
