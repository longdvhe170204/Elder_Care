package com.eldercare.backend.care.web.request;

import java.time.Instant;

/** 014 T009: thân lệnh {@code /activity-sessions/{id}/{command}} của api-014. */
public record ActivitySessionCommandWrite(
        String reason, Instant startsAt, Instant endsAt, Long locationId, Long responsibleStaffId) {}
