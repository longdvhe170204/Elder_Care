package com.eldercare.backend.care.internal.governance.domain;

import java.time.Instant;

/** Ảnh đọc lưu giữ: mốc hồ sơ lấy qua API001, mốc không gắn hồ sơ lấy từ plannedAt. */
public record CareRetentionRow(long id, Long residentId, Instant plannedAt) {}
