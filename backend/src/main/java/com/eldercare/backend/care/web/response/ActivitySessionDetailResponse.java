package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.internal.session.service.ActivitySessionService;
import java.time.Instant;
import java.util.List;

/** 014 T009: chi tiết buổi kèm đăng ký còn hiệu lực (schema {@code ActivitySessionDetail}); điểm danh thêm ở US3. */
public record ActivitySessionDetailResponse(
        long id,
        long activityId,
        Instant startsAt,
        Instant endsAt,
        String status,
        boolean incompleteAttendance,
        Long floorAreaId,
        Long responsibleStaffId,
        String cancelReason,
        int version,
        List<SessionRegistration> registrations) {
    public record SessionRegistration(long id, long residentId, String source, String status) {}

    public static ActivitySessionDetailResponse of(ActivitySessionService.Detail d) {
        var s = d.session();
        return new ActivitySessionDetailResponse(
                s.id(),
                s.activityId(),
                s.startsAt(),
                s.endsAt(),
                s.status(),
                s.incompleteAttendance(),
                s.floorAreaId(),
                s.responsibleStaffId(),
                s.cancelReason(),
                s.version(),
                d.registrations().stream()
                        .map(r -> new SessionRegistration(r.id(), r.residentId(), r.source(), r.status()))
                        .toList());
    }
}
