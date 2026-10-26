package com.cambae.backend.m04.api.dto;

import com.cambae.backend.common.Role;

public record CareAssignmentView(
        long assignmentId, long staffId, Role role, String assignmentKind, Long taskTypeId, boolean assignedNurse) {}
