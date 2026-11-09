package com.eldercare.backend.care.web.request;

import com.eldercare.backend.care.internal.activity.domain.Activity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/** 014 T004: thân {@code Activity} của api-014 (glossary 20.3). */
public record ActivityWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long typeId,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"FLOOR", "FACILITY"})
        String scope,

        Long floorAreaId,
        Location defaultLocation,
        Long defaultResponsibleStaffId,
        Integer durationMinutes,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        Integer maxParticipants,

        Map<String, Object> targetGroup,
        Boolean charged,
        Long serviceId,
        Boolean groupActivity,
        String groupActivityChangeReason,
        List<Long> registrationsToCancel,
        String reason) {
    public record Location(Long roomId, Long areaId, String destination) {}

    public Activity toActivity() {
        return new Activity(
                name,
                typeId == null ? 0 : typeId,
                scope,
                floorAreaId,
                defaultLocation == null
                        ? null
                        : new Activity.Location(
                                defaultLocation.roomId(), defaultLocation.areaId(), defaultLocation.destination()),
                defaultResponsibleStaffId,
                durationMinutes,
                maxParticipants == null ? 0 : maxParticipants,
                targetGroup,
                Boolean.TRUE.equals(charged),
                serviceId,
                false,
                groupActivityChangeReason,
                0,
                null);
    }
}
