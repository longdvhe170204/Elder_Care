package com.cambae.backend.m01.api.events;

import java.util.Optional;

/** Hồ sơ được tạo; 004 mở lượt đăng ký, 016 nghe (FR-001, FR-004). */
public record ResidentCreated(long residentId, Optional<Long> previousProfileId) {}
