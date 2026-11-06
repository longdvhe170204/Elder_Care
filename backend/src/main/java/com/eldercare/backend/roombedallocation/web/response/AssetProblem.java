package com.eldercare.backend.roombedallocation.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/** Chỉ mô tả schema {@code Problem} của api-019 cho OpenAPI; thân lỗi thật do handler chung dựng. */
@Schema(name = "AssetProblem")
public record AssetProblem(
        String type,
        String title,
        Integer status,
        String detail,
        String errorCode,
        List<Map<String, Object>> unmetConditions) {}
