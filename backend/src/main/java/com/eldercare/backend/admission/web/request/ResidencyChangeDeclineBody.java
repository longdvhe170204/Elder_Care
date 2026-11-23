package com.eldercare.backend.admission.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Người đại diện không đồng ý yêu cầu thay đổi lưu trú, kèm lý do bắt buộc (FR-043c). */
@Schema(name = "ResidencyChangeDecline")
public record ResidencyChangeDeclineBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = String.class)
        Reason reason) {}
