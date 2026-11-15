package com.eldercare.backend.care.api.dto;

import java.time.Instant;

/** FR-068a: registered là đã có tên trong đăng ký còn hiệu lực. */
public record ResidentActivitySessionView(
        long sessionId, String name, Instant startAt, Instant endAt, boolean facilityWide, boolean registered) {}
