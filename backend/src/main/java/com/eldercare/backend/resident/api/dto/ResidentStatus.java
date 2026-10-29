package com.eldercare.backend.resident.api.dto;

/** Trạng thái người cao tuổi (mục 5.5; 001 data-model). */
public enum ResidentStatus {
    ADMISSION_IN_PROGRESS,
    IN_RESIDENCE,
    TEMPORARILY_ABSENT,
    OFF_SITE_ACTIVITY,
    HOSPITALIZED,
    RESIDENCY_ENDED,
    DECEASED,
    ADMISSION_CANCELLED;
}
