package com.eldercare.backend.care.web.request;

import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Bản dịch T051 được chủ dự án duyệt ngày02/11/2026. */
public record CareResultCorrectionWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CorrectionType type,
        Object correctedContent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
