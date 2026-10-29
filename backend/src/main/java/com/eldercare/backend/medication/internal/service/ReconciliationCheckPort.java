package com.eldercare.backend.medication.internal.service;

/**
 * Medication reconciliation check port (FR-037).
 * When an open or pending confirmation reconciliation ticket exists, blocks new prescription creation (409 DANG_CO_PHIEU_DOI_CHIEU).
 */
public interface ReconciliationCheckPort {

    boolean hasOpenOrPendingReconciliation(long residentId);
}
