package com.eldercare.backend.billing.api.dto;

/** 019 FR-008: trạng thái yêu cầu mua sắm; RECONCILED, REJECTED, CANCELLED là trạng thái cuối. */
public enum ProcurementStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    PENDING_RECONCILIATION,
    DISCREPANCY,
    RECONCILED,
    REJECTED,
    CANCELLED
}
