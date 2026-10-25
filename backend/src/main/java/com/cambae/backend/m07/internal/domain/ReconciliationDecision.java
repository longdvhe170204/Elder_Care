package com.cambae.backend.m07.internal.domain;

/**
 * Clinical decision on reconciliation item line (FR-036).
 */
public enum ReconciliationDecision {
    /** Continue current prescription */
    CONTINUE,
    /** Discontinue current prescription */
    DISCONTINUE,
    /** Change dosage or administration instruction */
    CHANGE_DOSE,
    /** Add new prescription from discharge order or admission */
    ADD_NEW
}
