package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Thân lệnh lựa chọn thực hiện (api-007 {@code POST /alerts/{id}/execute-choice}; FR-050d). {@code transferOut} dùng
 * cho {@code TRANSFER_TO_HOSPITAL} khi lệnh Chuyển viện của US5 có (T040 → T042).
 */
public record ExecuteChoiceRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"TRANSFER_TO_HOSPITAL", "RETURN_HOME", "STAY_PALLIATIVE"})
        String choice,

        Reason reason,
        TransferOutBody transferOut) {}
