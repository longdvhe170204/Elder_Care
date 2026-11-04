package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh đóng cảnh báo: kết quả bắt buộc; lý do với cảnh báo công việc Bắt buộc quá hạn (BR-M04-06). */
public record CloseAlertRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
        String resolution,

        Reason reason) {}
