package com.eldercare.backend.medication.web.response;

import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.medication.web.request.DoseRequests;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Thân lỗi 409 riêng của api-006 cho OpenAPI sinh ra: schema {@code Problem} chung cộng trường riêng của từng phản hồi
 * (contracts api-006 {@code components.responses}); thân lỗi thật vẫn do {@code ProblemDetailsHandler} dựng từ chi tiết
 * của {@code BusinessException}.
 */
public final class M07Problems {
    private M07Problems() {}

    /** {@code PRN_LIMIT_EXCEEDED} (BR-M07-04): thời điểm sớm nhất được dùng và điều kiện vi phạm. */
    @Schema(
            name = "PrnProblem",
            requiredProperties = {"type", "title", "status", "errorCode"})
    public record Prn(
            @Schema(format = "uri") String type,
            String title,
            Integer status,
            String detail,
            String errorCode,
            List<UnmetCondition> unmetConditions,
            Object currentValue,
            List<Object> candidates,

            @Schema(allowableValues = {"ROLE", "SCOPE", "LEGAL", "SOURCE_DATA"})
            String failedLayer,

            ProblemBody.License license,
            Instant unlocksAt,
            Instant earliestAllowedAt,

            @io.swagger.v3.oas.annotations.media.ArraySchema(
                    schema = @Schema(allowableValues = {"MIN_INTERVAL", "MAX_PER_24H"}))
            List<String> violations) {}

    /**
     * {@code DOSE_ALREADY_CLOSED} (DBR-13): người đóng, thời điểm, trạng thái hiện hành của liều ({@code currentStatus},
     * vì {@code status} là mã HTTP của Problem) và nội dung bị từ chối để chuyển thành đính chính.
     */
    @Schema(
            name = "DoseAlreadyClosedProblem",
            requiredProperties = {"type", "title", "status", "errorCode"})
    public record DoseAlreadyClosed(
            @Schema(format = "uri") String type,
            String title,
            Integer status,
            String detail,
            String errorCode,
            List<UnmetCondition> unmetConditions,
            Object currentValue,
            List<Object> candidates,

            @Schema(allowableValues = {"ROLE", "SCOPE", "LEGAL", "SOURCE_DATA"})
            String failedLayer,

            ProblemBody.License license,
            Instant unlocksAt,
            String closedBy,
            Instant closedAt,

            @Schema(
                    allowableValues = {
                        "UPCOMING",
                        "DUE",
                        "LATE",
                        "MISSED",
                        "ADMINISTERED",
                        "REFUSED",
                        "NOT_ADMINISTERED",
                        "SUSPENDED",
                        "LEAVE_MEDICATION",
                        "CANCELLED"
                    })
            String currentStatus,

            DoseRequests.DoseResultRequest rejectedContent) {}

    /** {@code ISSUES_REQUIRE_CONFIRMATION} (FR-007, BR-M07-06): vấn đề trùng hoạt chất, dị ứng chưa xác nhận. */
    @Schema(
            name = "IssuesProblem",
            requiredProperties = {"type", "title", "status", "errorCode"})
    public record Issues(
            @Schema(format = "uri") String type,
            String title,
            Integer status,
            String detail,
            String errorCode,
            List<UnmetCondition> unmetConditions,
            Object currentValue,
            List<Object> candidates,

            @Schema(allowableValues = {"ROLE", "SCOPE", "LEGAL", "SOURCE_DATA"})
            String failedLayer,

            ProblemBody.License license,
            Instant unlocksAt,
            List<PrescriptionBodies.IssueBody> issues) {}
}
