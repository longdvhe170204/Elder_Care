package com.eldercare.backend.care.web.response;

import com.eldercare.backend.resident.api.dto.OpinionSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(requiredProperties = {"source", "content"})
public record RiskOpinionResponse(OpinionSource source, String content, String recordedBy, Instant at) {}
