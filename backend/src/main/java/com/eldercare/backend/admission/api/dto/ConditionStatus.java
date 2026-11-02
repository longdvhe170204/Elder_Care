package com.eldercare.backend.admission.api.dto;

/** Trạng thái một điều kiện tiếp nhận hoặc kết thúc (FR-004, FR-063). */
public enum ConditionStatus {
    MET,
    NOT_MET,
    NOT_APPLICABLE,
    MET_BY_EXCEPTION,
    AUTOMATIC;
}
