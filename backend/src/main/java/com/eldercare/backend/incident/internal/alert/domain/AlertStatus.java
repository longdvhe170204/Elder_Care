package com.eldercare.backend.incident.internal.alert.domain;

/** Trạng thái vòng đời cảnh báo (FR-029). */
public enum AlertStatus {
    NEW,
    ESCALATED,
    RECEIVED,
    IN_PROGRESS,
    CLOSED,
    CONVERTED_TO_INCIDENT
}
