package com.eldercare.backend.family.api.events;

import java.time.Instant;

/** Quan hệ người thân đã đổi ({@code QuanHeDaDoi}; FR-006); 002 xóa đệm quyền, 009, 013 nghe. */
public record RelationshipChanged(
        long familyMemberId, long residentId, long relationshipId, RelationshipChange change, Instant at) {}
