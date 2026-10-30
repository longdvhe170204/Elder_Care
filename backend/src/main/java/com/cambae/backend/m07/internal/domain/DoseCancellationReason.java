package com.cambae.backend.m07.internal.domain;

/**
 * Reason for cancelling a medication dose (data-model section 5, FR-021).
 */
public enum DoseCancellationReason {
    DISCONTINUED, // NGUNG_DON
    REPLACED, // DOI_LIEU
    EXPIRED, // HET_HAN
    RECONCILIATION, // PHIEU_DOI_CHIEU
    FINAL_STATE // TRANG_THAI_CUOI
}
