package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.web.request.CareResultWrite;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Mô tả lỗi của 005 theo schema {@code CareCatalogProblem} của api-005; chỉ dùng cho OpenAPI, lỗi thật vẫn do
 * ProblemDetailsHandler trả (như {@code M09Problem} của 008).
 */
@Schema(name = "CareCatalogProblem")
public record CareProblemBody(
        String type, String title, Integer status, String detail, String errorCode, List<Condition> unmetConditions) {

    @Schema(name = "CareUnmetCondition")
    public record Condition(String code, String description, String basis) {}

    /**
     * {@code CareTaskClosed}: công việc đã đóng, giữ nội dung để chuyển thành đính chính hoặc Ghi nhận phát sinh.
     * {@code status} là mã HTTP, trạng thái công việc ở {@code currentStatus} (chủ dự án chốt 2026-11-10).
     */
    @Schema(name = "CareTaskClosedProblem")
    public record TaskClosed(
            String type,
            String title,
            Integer status,
            String detail,
            String errorCode,
            List<Condition> unmetConditions,
            TaskStatus currentStatus,
            String closedBy,
            Instant closedAt,
            CareResultWrite rejectedContent) {}

    /** {@code CareTaskRowProblem}: lô lập công việc bị từ chối, liệt kê lỗi theo dòng (research C-07). */
    @Schema(name = "CareTaskRowProblem")
    public record RowErrors(
            String type,
            String title,
            Integer status,
            String detail,
            String errorCode,
            List<Condition> unmetConditions,
            List<RowError> rowErrors) {}

    @Schema(name = "CareTaskRowError")
    public record RowError(@Schema(minimum = "1") Integer row, List<String> codes) {}
}
