package com.eldercare.backend.roombedallocation.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** T056: lý do bắt buộc, sự cố là tham chiếu logic tùy chọn. */
public record RoomIsolationWrite(
        @Schema(implementation = String.class, requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason,

        Long incidentId) {}
