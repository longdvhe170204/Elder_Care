package com.eldercare.backend.incident.api.dto;

import com.eldercare.backend.common.Role;
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
