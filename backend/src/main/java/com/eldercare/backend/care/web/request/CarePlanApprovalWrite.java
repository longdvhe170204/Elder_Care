package com.eldercare.backend.care.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh Duyệt kế hoạch theo api-005: lý do không bắt buộc, khác {@link CareCatalogReason}. */
@Schema(name = "CarePlanApproval")
public record CarePlanApprovalWrite(
        @Schema(implementation = String.class, minLength = 1, maxLength = 2000)
        Reason reason) {}
