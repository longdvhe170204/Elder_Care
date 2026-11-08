package com.eldercare.backend.care.internal.activity.domain;

import java.time.LocalDate;
import java.util.Map;

/** 014 FR-002 (data-model mục 2 {@code hoat_dong}): nội dung dòng {@code M04.ACTIVITY} của {@code simple_record}. */
public record Activity(
        String name,
        long typeId,
        String scope,
        Long floorAreaId,
        Location defaultLocation,
        Long defaultResponsibleStaffId,
        Integer durationMinutes,
        int maxParticipants,
        Map<String, Object> targetGroup,
        boolean charged,
        Long serviceId,
        boolean groupActivity,
        String groupActivityChangeReason,
        long createdByStaffId,
        LocalDate deactivatedFrom) {
    public static final String FLOOR = "FLOOR";
    public static final String FACILITY = "FACILITY";

    public record Location(Long roomId, Long areaId, String destination) {}

    public Activity deactivated(LocalDate from) {
        return new Activity(
                name,
                typeId,
                scope,
                floorAreaId,
                defaultLocation,
                defaultResponsibleStaffId,
                durationMinutes,
                maxParticipants,
                targetGroup,
                charged,
                serviceId,
                groupActivity,
                groupActivityChangeReason,
                createdByStaffId,
                from);
    }
}
