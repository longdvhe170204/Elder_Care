package com.eldercare.backend.health.api.dto;

import java.time.Instant;

/** Lần đo đã xác nhận cho thống kê, mức lúc đo theo thành phần nặng nhất (FR-081). */
public record VitalSignReportView(
        long recordId, long residentId, long indicatorId, Long floorAreaId, Instant measuredAt, VitalSignLevel level) {}
