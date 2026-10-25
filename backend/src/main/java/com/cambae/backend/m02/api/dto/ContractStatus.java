package com.cambae.backend.m02.api.dto;

/** Trạng thái hợp đồng (FR-019 → FR-029). */
public enum ContractStatus {
    DRAFT,
    PENDING_SIGNATURE,
    ACTIVE,
    ENDED,
    TERMINATED,
    CANCELLED;
}
