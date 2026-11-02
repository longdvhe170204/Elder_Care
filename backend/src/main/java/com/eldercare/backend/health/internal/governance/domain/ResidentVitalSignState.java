package com.eldercare.backend.health.internal.governance.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/** Trạng thái kỹ thuật M06 theo người; thay chi_so_nct theo R-15, T007. */
public record ResidentVitalSignState(Instant missingThresholdRemindedAt) {
    public static final SharedKind<ResidentVitalSignState> KIND =
            SharedKind.of("M06.RESIDENT_VITAL_SIGNS", ResidentVitalSignState.class);
}
