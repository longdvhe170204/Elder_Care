package com.eldercare.backend.family.web.response;

import com.eldercare.backend.family.internal.feedback.service.FeedbackService.AssignableView;
import io.swagger.v3.oas.annotations.media.Schema;

/** Người nhận giao lại được (api-012 {@code AssignableStaff}, đổi api 2026-11-05; FR-060). */
@Schema(name = "AssignableStaff")
public record AssignableStaffResponse(long accountId, String fullName, Role role) {

    public enum Role {
        FLOOR_MANAGER,
        NURSING_STAFF,
        OFFICE_STAFF
    }

    public static AssignableStaffResponse of(AssignableView v) {
        return new AssignableStaffResponse(
                v.accountId(), v.fullName(), Role.valueOf(v.role().name()));
    }
}
