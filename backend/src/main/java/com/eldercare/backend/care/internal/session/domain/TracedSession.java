package com.eldercare.backend.care.internal.session.domain;

import java.time.Instant;

/** 007 FR-060 (b): buổi hoặc chuyến chồng khoảng truy vết, với khoảng thời gian dùng để so chồng. */
public record TracedSession(long id, Instant from, Instant to) {}
