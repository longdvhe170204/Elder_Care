package com.eldercare.backend.incident.internal.alert.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** Một lần app gửi vị trí GPS (api-007 {@code LocationReport}; FR-047h): vĩ độ, kinh độ, sai số mét, thời điểm máy đo. */
public record AlertLocationReport(
        BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyMeters, Instant measuredAt) {}
