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

    /**
     * Hạn mới khi đang leo thang hoặc nhắc lại: như {@link #of}, nhưng mức Nhẹ không còn ca hiện tại hoặc ca kế tiếp (ví dụ
     * ngay sau khi hết ca) thì lấy bây giờ + CFG-M05-01 để cảnh báo, sự cố không kẹt ở cấp cũ.
     */
    public static Instant forEscalation(
            IncidentLevel level,
            Instant now,
            Long floorAreaId,
            ParameterApi parameters,
            Optional<AlertStaffPort> staffPort) {
        try {
            return of(level, now, floorAreaId, parameters, staffPort);
        } catch (IllegalStateException noShift) {
            if (level != IncidentLevel.LOW) throw noShift;
            return now.plus(parameters.read(ParameterCode.CFG_M05_01).asDuration());
        }
    }

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
        var current = shifts.shiftAt(floorAreaId, now);
        if (current.isEmpty()) {
            // Chủ dự án chốt 2026-11-23 (A2): tầng không có ca hiện tại thì hạn là hết ca kế tiếp
            return nextShiftEndOrMedium(shifts, floorAreaId, now, parameters);
        }
        var handoverLead = parameters.read(ParameterCode.CFG_M09_04).asDuration();
        if (now.isAfter(current.get().endsAt().minus(handoverLead))) {
            return nextShiftEndOrMedium(shifts, floorAreaId, now, parameters);
        }
        return current.get().endsAt();
    }

    /** Hết ca kế tiếp của tầng; chưa có ca kế tiếp thì theo CFG-M05-01 (chủ dự án chốt 2026-11-23, A2). */
    private static Instant nextShiftEndOrMedium(
            AlertStaffPort shifts, long floorAreaId, Instant now, ParameterApi parameters) {
        return shifts.nextShift(floorAreaId, now)
                .map(next -> next.endsAt())
                .orElseGet(
                        () -> now.plus(parameters.read(ParameterCode.CFG_M05_01).asDuration()));
    }
}
