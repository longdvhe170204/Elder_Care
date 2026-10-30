package com.eldercare.backend.medication.api.dto;

import java.time.Instant;

/** Nguồn 006 mục 1.4; khóa liều có thể chưa có dòng vật hóa. */
public record DoseHandoverView(
        String key, long residentId, Long roomId, String description, Instant plannedAt, String status) {}
