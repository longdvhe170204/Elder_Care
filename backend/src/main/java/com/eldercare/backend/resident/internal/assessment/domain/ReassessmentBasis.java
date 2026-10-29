package com.eldercare.backend.resident.internal.assessment.domain;

/** Căn cứ một lần báo đánh giá lại (FR-040; research H-11). */
public enum ReassessmentBasis {
    PERIODIC,
    AFTER_INCIDENT,
    RETURN_FROM_HOSPITAL,
    ASSESSMENT_CORRECTION;
}
