package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.emergency.domain.Call115Info;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** {@code Call115Info} của api-007 (FR-047k); {@code location} là vị trí GPS mới nhất của cảnh báo (FR-047h). */
@Schema(name = "Call115Info")
public record Call115InfoResponse(
        Facility facility,
        String floor,
        String roomOrPlace,
        LocationResponse location,
        Resident resident,
        List<String> allergies,
        List<String> chronicConditions,
        List<String> hiddenFields) {

    public record Facility(String name, String address, String phone) {}

    public record Resident(String fullName, Integer age, String gender) {}

    public static Call115InfoResponse of(Call115Info info) {
        return new Call115InfoResponse(
                new Facility(
                        info.facility().name(),
                        info.facility().address(),
                        info.facility().phone()),
                info.floor(),
                info.roomOrPlace(),
                LocationResponse.of(info.location()),
                info.resident() == null
                        ? null
                        : new Resident(
                                info.resident().fullName(),
                                info.resident().age(),
                                info.resident().gender()),
                info.allergies(),
                info.chronicConditions(),
                info.hiddenFields());
    }
}
