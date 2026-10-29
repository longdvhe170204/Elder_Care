package com.eldercare.backend.staffing.web.response;

import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Mô tả lỗi của 008: dùng nguyên schema chung 000, thêm cảnh báo theo hợp đồng đã duyệt. */
@Schema(name = "M09Problem")
public record M09ProblemBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri")
        String type,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer status,
        String detail,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String errorCode,
        List<UnmetCondition> unmetConditions,
        Object currentValue,
        List<Object> candidates,

        @Schema(allowableValues = {"ROLE", "SCOPE", "LEGAL", "SOURCE_DATA"})
        String failedLayer,

        ProblemBody.License license,
        Instant unlocksAt,
        List<M09Warning> warnings) {
    /** Chỉ mô tả schema; lỗi thật vẫn do ProblemDetailsHandler xử lý. */
    public record M09Warning(
            @Schema(
                    allowableValues = {
                        "CONTINUOUS_WORK_HOURS",
                        "REQUIRED_TRAINING_EXPIRED",
                        "BELOW_STAFFING_THRESHOLD",
                        "TASK_TYPE_REQUIREMENTS_NOT_MET",
                        "APPROVED_LEAVE_DAY"
                    })
            String code,

            String description,
            Map<String, Object> details) {}
}
