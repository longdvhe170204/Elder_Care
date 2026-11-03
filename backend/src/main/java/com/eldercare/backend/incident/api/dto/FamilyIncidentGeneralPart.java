package com.eldercare.backend.incident.api.dto;

import java.time.Instant;

/** Phần chung không chứa chẩn đoán, chỉ số, thuốc hay nội dung điều trị (research S-19a). */
public record FamilyIncidentGeneralPart(String typeName, Instant occurredAt, String level, String processingStatus) {}
