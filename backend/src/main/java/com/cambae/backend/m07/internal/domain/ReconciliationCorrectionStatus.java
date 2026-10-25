package com.cambae.backend.m07.internal.domain;

/**
 * Status of medication reconciliation correction request (FR-039, BR-M07-17).
 */
public enum ReconciliationCorrectionStatus {
    PENDING_CONFIRMATION,
    CONFIRMED,
    REJECTED
}
