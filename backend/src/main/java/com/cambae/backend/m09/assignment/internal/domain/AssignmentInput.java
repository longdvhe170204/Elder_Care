package com.cambae.backend.m09.assignment.internal.domain;

import com.cambae.backend.common.Role;
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
