package com.eldercare.backend.medication.internal.domain;

/**
 * Result type for a closed medication dose (data-model section 5, FR-021).
 */
public enum DoseResultType {
    ADMINISTERED, // DA_DUNG
    REFUSED, // TU_CHOI
    NOT_ADMINISTERED, // KHONG_THUC_HIEN
    MISSED // BO_LO
}
