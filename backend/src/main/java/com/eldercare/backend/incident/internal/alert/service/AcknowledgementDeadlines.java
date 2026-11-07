package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import java.time.Instant;
import java.util.Optional;

/** Hạn tiếp nhận theo mức (FR-031, research S-08), dùng chung cho cảnh báo và sự cố (FR-044). */
public final class AcknowledgementDeadlines {
    private AcknowledgementDeadlines() {}

    public static Instant of(
            IncidentLevel level,
            Instant now,
            Long floorAreaId,
            ParameterApi parameters,
            Optional<AlertStaffPort> staffPort) {
        if (level == IncidentLevel.CRITICAL) {
            return null; // Khẩn cấp không có hạn tiếp nhận, phát báo động ngay (FR-031)
        }
        if (level == IncidentLevel.MEDIUM) {
            return now.plus(parameters.read(ParameterCode.CFG_M05_01).asDuration());
        }
        if (floorAreaId == null || staffPort.isEmpty()) {
            throw new IllegalStateException("Chưa có nguồn ca để tính hạn cảnh báo Nhẹ");
        }
        var shifts = staffPort.get();
        var current = shifts.shiftAt(floorAreaId, now)
                .orElseThrow(() -> new IllegalStateException("Chưa có ca hiện tại của tầng"));
        var handoverLead = parameters.read(ParameterCode.CFG_M09_04).asDuration();
        if (now.isAfter(current.endsAt().minus(handoverLead))) {
            return shifts.nextShift(floorAreaId, now)
                    .orElseThrow(() -> new IllegalStateException("Chưa có ca kế tiếp của tầng"))
                    .endsAt();
        }
        return current.endsAt();
    }
}
