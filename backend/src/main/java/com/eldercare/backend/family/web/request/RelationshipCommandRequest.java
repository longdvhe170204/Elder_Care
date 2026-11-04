package com.eldercare.backend.family.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Thân lệnh trên quan hệ: lý do; bằng chứng khi đặt người đại diện đầu tiên; thứ tự khi đổi thứ tự liên hệ. */
public record RelationshipCommandRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason,

        Long evidenceFileId,
        Integer contactOrder) {}
