package com.eldercare.backend.roombedallocation.api.dto;
/** 019 FR-018: chỉ tác động kèm lệnh chuyến, không tự tạo lịch xe. */
public enum TripVehicleChangeType {
    CHANGED,
    CANCELLED,
    FIRST_DEPARTURE,
    EXTENDED,
    LAST_DEPARTURE_UNDONE,
    RETURN_FINISHED
}
