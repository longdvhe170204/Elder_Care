package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Lý do gỡ khoanh vùng. */
public record IsolationReasonRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
