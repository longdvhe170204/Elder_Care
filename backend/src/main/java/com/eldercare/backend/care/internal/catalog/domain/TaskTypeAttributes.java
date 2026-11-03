package com.eldercare.backend.care.internal.catalog.domain;

import com.eldercare.backend.common.Role;
import jakarta.validation.constraints.*;
import java.util.Set;

/** Dữ liệu riêng của loại công việc; null ở khung nghĩa là đọc CFG khi lập mục. */
public record TaskTypeAttributes(
        @NotNull
        @Pattern(
                regexp =
                        "PERSONAL_HYGIENE|BATHING|DRESSING|EATING_ASSISTANCE|DRINKING_ASSISTANCE|TOILETING_ASSISTANCE|MOVEMENT|REHABILITATION|VITAL_SIGN_MEASUREMENT|ACTIVITY|SLEEP|MOOD|BEHAVIOR|DIAPER_ITEM_CHANGE|SHIFT_START_CHECK|SUPERVISION|MEDICATION|OTHER")
        String group,

        boolean professional,

        @NotNull @Pattern(regexp = "EATING|ML|MEASURED_VALUE|MOVEMENT|SLEEP|MOOD|BEHAVIOR|ITEM_QUANTITY|PERFORMED|NONE")
        String resultType,

        @NotNull Role defaultRole,

        @NotNull @Pattern(regexp = "NORMAL|IMPORTANT|MANDATORY")
        String defaultImportance,

        @Min(-32768) @Max(32767) Integer windowBeforeMinutes,
        @Min(-32768) @Max(32767) Integer windowAfterMinutes,
        boolean billable,
        Long serviceId,
        Integer upperLimit,
        @Size(max = 100) String vitalSignType,
        Set<@NotNull @Size(max = 100) String> certificates,
        LegacyCatalogId legacy) {
    public TaskTypeAttributes {
        certificates = certificates == null ? Set.of() : Set.copyOf(certificates);
    }
}
