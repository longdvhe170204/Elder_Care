package com.eldercare.backend.medication.internal.domain;

/**
 * Status of carried medication handover (FR-048a).
 */
public enum HandoverStatus {
    HANDED_OVER, // CARRIED_OUT
    WAITING_RETURN, // AWAITING_RETURN
    RETURNED // RETURNED
}
