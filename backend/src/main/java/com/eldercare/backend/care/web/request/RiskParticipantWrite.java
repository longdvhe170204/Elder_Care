package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Người cao tuổi tham gia được thể hiện bằng residentParticipates của thỏa thuận. */
@Schema(requiredProperties = {"type", "id"})
public record RiskParticipantWrite(String type, long id) {}
