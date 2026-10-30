package com.eldercare.backend.roombedallocation.internal.allocation.domain;

/** Vòng đời phân bổ theo FR-017, chỉ đổi qua lệnh nghiệp vụ. */
public enum AllocationStatus {
    FUTURE,
    ACTIVE,
    CLOSED,
    CANCELLED
}
