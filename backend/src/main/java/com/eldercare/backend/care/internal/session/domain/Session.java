package com.eldercare.backend.care.internal.session.domain;

import java.time.Instant;

/** 014 T008 ({@code buoi_hoat_dong}, data-model mục 3): một dòng {@code activity_session}. */
public record Session(
        long id,
        long activityId,
        Instant startsAt,
        Instant endsAt,
        String source,
        Long locationRoomId,
        Long locationAreaId,
        Long floorAreaId,
        Long responsibleStaffId,
        String status,
        boolean incompleteAttendance,
        String cancelReason,
        String cancelledBy,
        int version,
        String tripStatus) {
    public Session(
            long id,
            long activityId,
            Instant startsAt,
            Instant endsAt,
            String source,
            Long locationRoomId,
            Long locationAreaId,
            Long floorAreaId,
            Long responsibleStaffId,
            String status,
            boolean incompleteAttendance,
            String cancelReason,
            String cancelledBy,
            int version) {
        this(
                id,
                activityId,
                startsAt,
                endsAt,
                source,
                locationRoomId,
                locationAreaId,
                floorAreaId,
                responsibleStaffId,
                status,
                incompleteAttendance,
                cancelReason,
                cancelledBy,
                version,
                null);
    }

    public static final String TEMPLATE = "TEMPLATE";
    public static final String SINGLE = "SINGLE";

    public static final String SCHEDULED = "SCHEDULED";
    public static final String ATTENDANCE_TAKEN = "ATTENDANCE_TAKEN";
    public static final String NO_ATTENDANCE = "NO_ATTENDANCE";
    public static final String CANCELLED = "CANCELLED";

    public static final String BY_STAFF = "STAFF";
    public static final String BY_TEMPLATE_CHANGE = "TEMPLATE_CHANGED";
    public static final String BY_DEACTIVATION = "DEACTIVATED";
    public static final String BY_ISOLATION = "ISOLATION";
}
