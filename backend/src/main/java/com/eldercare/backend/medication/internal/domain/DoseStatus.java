package com.eldercare.backend.medication.internal.domain;

/**
 * Lifecycle status of a medication dose (data-model section 5, FR-021).
 */
public enum DoseStatus {
    UPCOMING, // CHUA_DEN_GIO
    DUE, // DEN_GIO
    LATE, // TRE
    MISSED, // BO_LO
    ADMINISTERED, // DA_DUNG
    REFUSED, // TU_CHOI
    NOT_ADMINISTERED, // KHONG_THUC_HIEN
    SUSPENDED, // TAM_DUNG
    LEAVE_MEDICATION, // MANG_THEO
    CANCELLED // DA_HUY
}
