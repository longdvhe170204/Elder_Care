package com.eldercare.backend.staffing.internal.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

/** Nội dung danh mục M09 sau R-15; legacy chỉ phục vụ ánh xạ, không ra hợp đồng HTTP. */
public final class StaffingCatalogData {
    private StaffingCatalogData() {}

    public record Legacy(@NotBlank String table, @Positive long id) {
        public static long logicalId(long physicalId, Legacy legacy, String expectedTable) {
            if (legacy == null) return physicalId;
            if (!expectedTable.equals(legacy.table()) || legacy.id() <= 0)
                throw new IllegalStateException("Định danh legacy không khớp loại " + expectedTable);
            return legacy.id();
        }
    }

    public record DepartmentAttributes(Long leadStaffId, Instant createdAt, Legacy legacy) {}

    public record CredentialTypeAttributes(
            @Pattern(regexp = "PROFESSIONAL_LICENSE|TRAINING")
            String credentialGroup,

            boolean hasExpiry,
            Set<String> requiredForRoles,
            boolean hasPracticeScopes,
            String legacyCode,
            Instant createdAt,
            Legacy legacy) {}

    public record PracticeScopeAttributes(String legacyCode, Instant createdAt, Legacy legacy) {}

    public record ShiftTemplateAttributes(
            LocalTime startTime,
            LocalTime endTime,
            String scopeType,
            boolean requiresHandover,
            String originalName,
            Instant createdAt,
            Legacy legacy) {}

    public static final SharedKind<DepartmentAttributes> DEPARTMENT =
            SharedKind.of("M09.DEPARTMENT", DepartmentAttributes.class).notDeletable();
    public static final SharedKind<CredentialTypeAttributes> CREDENTIAL_TYPE =
            SharedKind.of("M09.CREDENTIAL_TYPE", CredentialTypeAttributes.class).notDeletable();
    public static final SharedKind<PracticeScopeAttributes> PRACTICE_SCOPE =
            SharedKind.of("M09.PRACTICE_SCOPE", PracticeScopeAttributes.class).notDeletable();
    public static final SharedKind<ShiftTemplateAttributes> SHIFT_TEMPLATE =
            SharedKind.of("M09.SHIFT_TEMPLATE", ShiftTemplateAttributes.class).notDeletable();
}
