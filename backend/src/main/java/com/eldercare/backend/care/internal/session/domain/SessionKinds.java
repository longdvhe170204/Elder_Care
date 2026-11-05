package com.eldercare.backend.care.internal.session.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * 014 ({@code diem_danh_buoi} → {@code record_event}, data-model mục 0, 5): kết quả điểm danh của một đăng ký, cha là hồ
 * sơ người cao tuổi (xóa cùng hồ sơ), {@code unique_key} {@code M04.SESSION_ATTENDANCE:<đăng ký>}. US1 chỉ ghi "Không ghi
 * nhận" lúc hết ngày (FR-014a); điểm danh đầy đủ ở US3 (T014).
 */
public final class SessionKinds {
    public static final SharedKind<AttendanceResult> ATTENDANCE =
            SharedKind.of("M04.SESSION_ATTENDANCE", AttendanceResult.class);

    public static final String NOT_RECORDED = "NOT_RECORDED";

    public static final String PRESENT = "PRESENT";
    public static final String ABSENT = "ABSENT";

    /** Kết quả điểm danh (data-model mục 5, glossary 20.3 {@code SessionAttendance}); {@code deviceAt} khi ghi ngoại tuyến. */
    public record AttendanceResult(
            long registrationId,
            long sessionId,
            long residentId,
            String result,
            String absenceReason,
            String participationLevel,
            String communicationLevel,
            String conditionAfter,
            String note,
            boolean lateRecorded,
            java.time.Instant deviceAt) {}

    public static String key(long registrationId) {
        return ATTENDANCE.code() + ":" + registrationId;
    }

    private SessionKinds() {}
}
