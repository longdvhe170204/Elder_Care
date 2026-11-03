package com.eldercare.backend.incident.internal.alert.domain;

/** Cho phép không gắn người cao tuổi là cờ chỉ đọc của loại có quy tắc riêng. */
public record IncidentTypeAttributes(IncidentLevel defaultLevel, boolean nonHealth, boolean allowsNoResident) {}
