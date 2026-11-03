package com.eldercare.backend.incident.internal.governance.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/** Đích khóa M05 theo người; phiên bản nằm ở resident_module_state (R-15, T007). */
public record ResidentMonitoringState() {
    public static final SharedKind<ResidentMonitoringState> KIND =
            SharedKind.of("M05.RESIDENT_MONITORING", ResidentMonitoringState.class);
}
