package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import io.swagger.v3.oas.annotations.media.Schema;

/** Yêu cầu nâng hoặc hạ mức sự cố. */
public record ChangeIncidentLevelRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) IncidentLevel newLevel,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
