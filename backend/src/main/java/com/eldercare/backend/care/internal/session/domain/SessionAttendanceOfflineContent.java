package com.eldercare.backend.care.internal.session.domain;

import java.util.List;

/** 014 T014 (Q-01): nội dung bản ghi ngoại tuyến {@code M04.SESSION_ATTENDANCE}, cùng trường với thân {@code SessionAttendance}. */
public record SessionAttendanceOfflineContent(long sessionId, List<Entry> results) {
    public record Entry(
            Long residentId,
            Boolean addedAtAttendance,
            String result,
            String absenceReason,
            String participationLevel,
            String communicationLevel,
            String conditionAfter,
            String note) {}
}
