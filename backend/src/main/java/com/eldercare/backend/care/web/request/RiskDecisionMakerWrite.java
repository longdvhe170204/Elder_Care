package com.eldercare.backend.care.web.request;

public record RiskDecisionMakerWrite(String type, Long familyMemberId, Long doctorId, String reason) {}
