package com.cambae.backend.m03.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** T017: thân lệnh theo hợp đồng Phase 3 đã duyệt. */
public record ReasonBody(
        @Schema(implementation = String.class, requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {}
