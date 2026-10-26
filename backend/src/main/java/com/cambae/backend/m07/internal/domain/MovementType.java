package com.cambae.backend.m07.internal.domain;

/**
 * Family medication inventory movement type (FR-045).
 */
public enum MovementType {
    RECEIPT,
    DOSE_USAGE,
    PRN_USAGE,
    DOSE_CORRECTION,
    HANDOVER_LEAVE,
    RETURN_HANDOVER,
    INVENTORY_ADJUSTMENT,
    RETURN,
    DISPOSAL
}
