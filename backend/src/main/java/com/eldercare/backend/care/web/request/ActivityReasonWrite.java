package com.eldercare.backend.care.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** 014: thân {@code requestBodies/Reason} của api-014 (hủy đăng ký, rút đồng ý về muộn). */
public record ActivityReasonWrite(
        @Schema(
                implementation = String.class,
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1,
                maxLength = 2000)
        Reason reason) {}
