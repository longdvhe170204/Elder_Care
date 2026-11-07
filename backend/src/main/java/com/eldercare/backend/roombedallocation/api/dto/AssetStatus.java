package com.eldercare.backend.roombedallocation.api.dto;

/** 019 FR-013: trạng thái tài sản; DISPOSED là trạng thái cuối. */
public enum AssetStatus {
    READY,
    IN_USE,
    DAMAGED,
    UNDER_MAINTENANCE,
    DISPOSED
}
