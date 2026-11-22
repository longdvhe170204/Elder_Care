package com.eldercare.backend.administration.approval.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh Trả về nháp (000 FR-041a (1); Q-419). */
public record ReturnToDraftBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
