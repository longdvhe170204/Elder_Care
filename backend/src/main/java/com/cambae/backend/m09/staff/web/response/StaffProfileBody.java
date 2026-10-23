package com.cambae.backend.m09.staff.web.response;

import com.cambae.backend.m09.staff.internal.domain.StaffProfile;
import com.cambae.backend.m15.account.api.*;
import com.cambae.backend.m15.account.api.dto.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** Schema StaffProfile: serializer của 002 bỏ liên hệ bị che và ghi tên vào hiddenFields. */
@Schema(name = "StaffProfile")
public record StaffProfileBody(
        long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fullName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String position,
        String specialty,
        LocalDate startDate,
        @FieldGroup(InfoGroup.STAFF_CONTACT) String phoneNumber,
        @FieldGroup(InfoGroup.STAFF_CONTACT) String email,

        @Schema(
                allowableValues = {"WORKING", "RESIGNED"},
                accessMode = Schema.AccessMode.READ_ONLY)
        String status,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long accountId,

        @Schema(
                allowableValues = {"MANUAL", "IMPORT"},
                accessMode = Schema.AccessMode.READ_ONLY)
        String source,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Long importBatchId,
        Long departmentId,
        List<String> hiddenFields)
        implements VisibilityTarget {
    public Target visibilityTarget() {
        return new Target("M09.STAFF", Long.toString(id), null, null);
    }

    public static StaffProfileBody of(StaffProfile s) {
        return new StaffProfileBody(
                s.id(),
                s.fullName(),
                s.position(),
                s.specialty(),
                s.startDate(),
                s.phoneNumber(),
                s.email(),
                s.status(),
                s.accountId(),
                s.source(),
                s.importBatchId(),
                s.departmentId(),
                List.of());
    }
}
