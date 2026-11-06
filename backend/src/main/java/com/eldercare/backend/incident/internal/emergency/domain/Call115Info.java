package com.eldercare.backend.incident.internal.emergency.domain;

import com.eldercare.backend.incident.internal.alert.domain.AlertLocationView;
import java.util.List;

/** Thông tin đọc cho tổng đài 115 (FR-047k; schema {@code Call115Info}); vị trí GPS mới nhất của cảnh báo (FR-047h). */
public record Call115Info(
        Facility facility,
        String floor,
        String roomOrPlace,
        AlertLocationView location,
        Resident resident,
        List<String> allergies,
        List<String> chronicConditions,
        List<String> hiddenFields) {

    public record Facility(String name, String address, String phone) {}

    public record Resident(String fullName, Integer age, String gender) {}
}
