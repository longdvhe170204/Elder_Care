package com.eldercare.backend.staffing.assignment.internal.domain;

import java.time.Instant;

public record FloorManagerAssignment(
        long id, long staffId, long floorAreaId, Instant startsAt, Instant endsAt, String status, String reason) {}
