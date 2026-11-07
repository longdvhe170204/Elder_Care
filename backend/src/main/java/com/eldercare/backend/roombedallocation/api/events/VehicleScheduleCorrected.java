package com.eldercare.backend.roombedallocation.api.events;

import java.time.Instant;

/**
 * 019 T022 (chủ dự án duyệt 2026-11-07): đính chính lịch xe {@code M03.VEHICLE_SCHEDULE} làm đổi giờ đi hoặc giờ về dự
 * kiến của lịch gắn chuyến đi; 014 nghe để đồng bộ giờ chuyến (care phụ thuộc M03 nên M03 không gọi ngược). Phát sau khi
 * kiểm chồng lịch, cùng transaction với lệnh đính chính; không gửi thông báo trực tiếp.
 */
public record VehicleScheduleCorrected(
        long scheduleId, long tripId, Instant plannedDepartureAt, Instant plannedReturnAt) {}
