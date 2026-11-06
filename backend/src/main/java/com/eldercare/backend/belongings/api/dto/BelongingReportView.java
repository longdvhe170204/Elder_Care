package com.eldercare.backend.belongings.api.dto;

import java.time.Instant;

/** Một dòng số liệu đồ gửi trong kỳ (013 contracts mục 1.3). */
public record BelongingReportView(
        BelongingReportKind kind, long belongingId, long residentId, long typeId, Instant at) {}
