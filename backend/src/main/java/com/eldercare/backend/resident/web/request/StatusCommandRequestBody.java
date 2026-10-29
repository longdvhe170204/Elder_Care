package com.eldercare.backend.resident.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Thân lệnh trạng thái (contracts/api-001.yaml, requestBodies {@code StatusCommandBody}). {@code attachmentFileIds}
 * chỉ nhận ở Ghi nhận trở về từ bệnh viện và Chuyển viện (FR-047c, T082).
 */
@Schema(name = "StatusCommandBody")
public record StatusCommandRequestBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason,

        @Size(max = 10) List<Long> attachmentFileIds,
        JsonNode details) {}
