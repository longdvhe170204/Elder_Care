package com.cambae.backend.m15.account.api.spi;

import java.time.Instant;

/**
 * Một phân công của nhân viên (FR-028 → FR-032; 002 research A-07). Đối tượng là một trong tầng/khu vực, phòng, người
 * cao tuổi (còn lại {@code null}); phân công gắn ca có {@code shiftId}, khoảng là giờ ca; không gắn ca thì khoảng hiệu lực
 * ({@code to} {@code null} khi chưa kết thúc).
 */
public record AssignmentView(
        AssignmentKind kind, Long floorAreaId, Long roomId, Long residentId, Long shiftId, Instant from, Instant to) {}
