package com.cambae.backend.common.offline;

import java.time.Instant;

/**
 * Phong bì của một bản ghi gửi từ app (research R-13; FR-046, DBR-25): khóa chống lặp, thời điểm trên thiết bị, dấu
 * ghi khi ngoại tuyến. Thời điểm đồng bộ do máy chủ đặt.
 */
public record OfflineEnvelope(String idempotencyKey, Instant deviceTime, boolean offline) {}
