package com.cambae.backend.m09.api.dto;

import java.time.Instant;

/** Kiểu đọc giữa module, theo 008 contracts mục 1.3/1.4; không chứa thông tin liên hệ hoặc lý do riêng tư. */
public record HandoverReportView(
        long handoverId,
        long shiftId,
        Long floorAreaId,
        String status,
        Instant preparedAt,
        Instant confirmedAt,
        boolean late,
        // 016 FR-020 (luật "Đổi api", chủ dự án duyệt 2026-10-27): mốc tính thời gian đã chờ.
        Instant shiftEndsAt) {}
