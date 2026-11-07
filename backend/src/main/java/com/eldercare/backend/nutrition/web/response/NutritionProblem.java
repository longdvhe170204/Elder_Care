package com.eldercare.backend.nutrition.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Schema {@code Problem} của api-011 cho OpenAPI sinh ra; thân lỗi thật do {@code ProblemDetailsHandler} của common dựng
 * (như {@code CareProblemBody} của 005).
 */
@Schema(name = "NutritionProblem")
public record NutritionProblem(
        String type, String title, Integer status, String detail, String errorCode, List<Object> unmetConditions) {}
