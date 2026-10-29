package com.eldercare.backend.roombedallocation.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** T027: người dùng chọn giường, không gửi loại bản ghi hoặc kết thúc dự kiến. */
public record BedAllocationWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long residentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long bedId,
        Instant startAt,

        @Schema(implementation = String.class, requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {}
