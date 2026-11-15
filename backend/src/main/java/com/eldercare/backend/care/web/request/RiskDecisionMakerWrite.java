package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"type"})
public record RiskDecisionMakerWrite(String type, Long familyMemberId, Long doctorId, String reason) {}
