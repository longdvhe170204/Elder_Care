package com.eldercare.backend.medication.internal.domain;

/**
 * Authority basis for recording a dose result (data-model section 5, FR-026, FR-027).
 */
public enum DoseAuthorityBasis {
    ASSIGNED, // DUOC_PHAN_CONG
    SHIFT_LEAD, // NGUOI_PHU_TRACH_CA
    FLOOR_SHARED, // LIEU_CHUNG_TANG
    LEAVE_MEDICATION, // MANG_THEO
    REPORTED_BY_OTHERS, // GHI_THEO_BAO_LAI
    SYSTEM // HE_THONG
}
