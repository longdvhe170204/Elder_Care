package com.eldercare.backend.incident.internal.emergency.domain;

import java.util.List;

/** Thông tin đọc cho tổng đài 115 (FR-047k; schema {@code Call115Info}). Vị trí GPS thêm ở T096. */
public record Call115Info(
        Facility facility,
        String floor,
        String roomOrPlace,
        Resident resident,
        List<String> allergies,
        List<String> chronicConditions,
        List<String> hiddenFields) {

    public record Facility(String name, String address, String phone) {}

    public record Resident(String fullName, Integer age, String gender) {}
}
