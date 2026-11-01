package com.eldercare.backend.care.internal.result.domain;

import java.time.Instant;

/** Giữ nội dung/nguồn thời gian gốc trong khi 007 chờ xác nhận đo lại. */
public record PendingCareResult(
        CareResultInput input,
        Instant performedAt,
        Instant recordedAt,
        Instant deviceAt,
        long accountId,
        long staffId,
        String permissionBasis,
        boolean late,
        boolean permissionLost) {}
