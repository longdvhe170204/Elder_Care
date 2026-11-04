package com.eldercare.backend.family.api.events;

import com.eldercare.backend.common.notification.FamilyRight;
import java.time.Instant;

/** Quyền của một quan hệ đã đổi từ {@code from} ({@code QuyenNguoiThanDaDoi}; FR-013); 002, 009, 013 nghe. */
public record FamilyRightChanged(
        long relationshipId, long familyMemberId, long residentId, FamilyRight right, boolean value, Instant from) {}
