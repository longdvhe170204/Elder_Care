package com.eldercare.backend.care.web.response;

public record RiskDecisionMakerResponse(String type, Long familyMemberId, Long doctorId, String reason) {}
