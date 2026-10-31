package com.eldercare.backend.care.web.request;

import com.eldercare.backend.resident.api.dto.OpinionSource;

/** recordedBy và at chỉ đọc, không lấy từ request. */
public record RiskOpinionWrite(OpinionSource source, String content) {}
