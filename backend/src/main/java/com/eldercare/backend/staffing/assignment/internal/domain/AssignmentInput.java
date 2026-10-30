package com.eldercare.backend.staffing.assignment.internal.domain;

import com.eldercare.backend.common.Role;
import java.time.Instant;

public record AssignmentInput(
        long staffId,
        String targetType,
        long targetId,
        Long taskTypeId,
        Role role,
        String assignmentKind,
        boolean assignedNurse,
        Instant startsAt,
        Instant endsAt) {}
