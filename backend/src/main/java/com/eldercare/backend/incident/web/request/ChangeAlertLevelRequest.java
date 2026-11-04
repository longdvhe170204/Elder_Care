package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh nâng, hạ mức cảnh báo (bảng FR-029, FR-038a). */
public record ChangeAlertLevelRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) IncidentLevel newLevel,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
