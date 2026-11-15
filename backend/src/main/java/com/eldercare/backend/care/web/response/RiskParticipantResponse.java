package com.eldercare.backend.care.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"type", "id"})
public record RiskParticipantResponse(String type, long id) {}
