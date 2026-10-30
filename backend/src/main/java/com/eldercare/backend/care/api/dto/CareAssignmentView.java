package com.eldercare.backend.care.api.dto;

import com.eldercare.backend.common.Role;

public record CareAssignmentView(
        long assignmentId, long staffId, Role role, String assignmentKind, Long taskTypeId, boolean assignedNurse) {}
