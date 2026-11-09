package com.eldercare.backend.roombedallocation.api.dto;

import java.util.List;

/** Kết quả kiểm lịch khi dời chuyến Q-239; điều kiện do 019 cung cấp. */
public record VehicleScheduleChangeCheck(boolean allowed, List<String> unmetConditions) {
    public VehicleScheduleChangeCheck {
        unmetConditions = List.copyOf(unmetConditions);
    }
}
