package com.eldercare.backend.care.web.request;

import com.eldercare.backend.resident.api.dto.OpinionSource;
import io.swagger.v3.oas.annotations.media.Schema;

/** recordedBy và at chỉ đọc, không lấy từ request. */
@Schema(requiredProperties = {"source", "content"})
public record RiskOpinionWrite(OpinionSource source, String content) {}
