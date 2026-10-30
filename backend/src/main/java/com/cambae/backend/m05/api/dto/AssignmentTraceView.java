package com.cambae.backend.m05.api.dto;

import com.cambae.backend.common.Role;
import java.time.Instant;

public record AssignmentTraceView(
        long assignmentId,
        long staffId,
        Role role,
        String assignmentKind,
        String targetType,
        long targetId,
        Instant from,
        Instant to) {}
