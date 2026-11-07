package com.eldercare.backend.roombedallocation.api.dto;

/** 019 FR-018: trạng thái lịch xe; COMPLETED, CANCELLED là trạng thái cuối. */
public enum VehicleScheduleStatus {
    BOOKED,
    IN_USE,
    COMPLETED,
    CANCELLED
}
