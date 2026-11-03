package com.eldercare.backend.care.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Thân CatalogReason đã duyệt; giá trị lý do được kiểm bằng khuôn Reason chung. */
public record CareCatalogReason(
        @Schema(
                implementation = String.class,
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1,
                maxLength = 2000)
        Reason reason) {}
