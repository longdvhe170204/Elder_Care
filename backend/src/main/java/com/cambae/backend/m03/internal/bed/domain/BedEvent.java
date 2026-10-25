package com.cambae.backend.m03.internal.bed.domain;
/** T008: dữ liệu của máy trạng thái giường, research G-04. */
public enum BedEvent {
    CREATE_BED,
    ALLOCATION_STARTED,
    HOLD_WAITING_RECORD,
    HOLD_EXPIRED,
    HOLD_CANCELLED,
    ABSENT_KEEP_BED,
    RETURNED,
    RELEASE_WHEN_ABSENT,
    RESIDENCY_ENDED,
    DEATH,
    TRANSFERRED_OUT,
    ABSENT_RELEASE_BED,
    CONFIRM_READY,
    ASSET_MAINTENANCE,
    ASSET_RETIRED,
    ASSET_READY,
    SUSPEND,
    REUSE
}
