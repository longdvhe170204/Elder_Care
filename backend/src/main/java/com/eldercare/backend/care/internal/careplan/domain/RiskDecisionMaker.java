package com.eldercare.backend.care.internal.careplan.domain;

/** Quyết định tầng 2 theo BR-M01-15; không phải dấu năng lực trên hồ sơ. */
public record RiskDecisionMaker(String type, Long familyMemberId, Long doctorId, String reason) {}
