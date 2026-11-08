package com.eldercare.backend.health.internal.threshold.domain;

import java.time.Instant;
import java.util.Map;

/** Một phiên bản ngưỡng (data-model {@code nguong_phien_ban}, R-15 → {@code monitoring_setting}, kind THRESHOLD). */
public record ThresholdVersion(
        long id,
        String scope,
        Long residentId,
        long indicatorId,
        Instant effectiveFrom,
        Instant effectiveUntil,
        String status,
        Map<String, ThresholdBand> components,
        int version) {}
