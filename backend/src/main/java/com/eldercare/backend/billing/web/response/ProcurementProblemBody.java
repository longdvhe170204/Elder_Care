package com.eldercare.backend.billing.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * Mô tả lỗi theo schema {@code Problem} của api-019; chỉ dùng cho OpenAPI, lỗi thật do ProblemDetailsHandler trả (như
 * {@code CareProblemBody} của 005).
 */
@Schema(name = "ProcurementProblem")
public record ProcurementProblemBody(
        String type,
        String title,
        Integer status,
        String detail,
        String errorCode,
        List<Map<String, Object>> unmetConditions) {}
