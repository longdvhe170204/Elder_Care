package com.eldercare.backend.care.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"type"})
public record RiskDecisionMakerResponse(String type, Long familyMemberId, Long doctorId, String reason) {}
