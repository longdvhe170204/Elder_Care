package com.eldercare.backend.care.web.response;

import com.eldercare.backend.resident.api.dto.OpinionSource;
import java.time.Instant;

public record RiskOpinionResponse(OpinionSource source, String content, String recordedBy, Instant at) {}
