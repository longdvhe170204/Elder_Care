package com.eldercare.backend.care.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Ngày đề xuất chỉ được nhập khi gửi duyệt. */
public record CarePlanSubmit(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate effectiveFrom, Reason changeReason) {}
