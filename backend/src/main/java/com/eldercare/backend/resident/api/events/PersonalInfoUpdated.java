package com.eldercare.backend.resident.api.events;

import java.util.Set;

/** Thông tin cá nhân đổi; 003 đánh dấu phòng không còn phù hợp khi giới tính đổi (003 FR-034; FR-006). */
public record PersonalInfoUpdated(long residentId, Set<String> changedFields) {}
