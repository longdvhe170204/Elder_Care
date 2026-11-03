package com.eldercare.backend.health.internal.catalog.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/** Loại danh mục riêng M06 trên bảng chung, R-15. */
public final class HealthCatalogs {
    public static final SharedKind<VitalSignAttributes> VITAL_SIGN =
            SharedKind.of("M06.VITAL_SIGN", VitalSignAttributes.class).notDeletable();

    public static final SharedKind<VitalSignComponentAttributes> VITAL_SIGN_COMPONENT = SharedKind.of(
                    "M06.VITAL_SIGN_COMPONENT", VitalSignComponentAttributes.class)
            .notDeletable();

    private HealthCatalogs() {}
}
