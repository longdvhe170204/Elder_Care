package com.eldercare.backend.care.internal.session.service;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.OfflineHandler;
import com.eldercare.backend.care.internal.session.domain.SessionAttendanceOfflineContent;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.offline.SyncOutcome;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 014 T014 (Q-01, FR-030; module-interfaces 014 mục 1.1 {@code NgoaiTuyenHandler} {@code M04.DIEM_DANH_BUOI}): điểm danh
 * buổi ghi được khi mất mạng; 002 kiểm quyền theo thời điểm thiết bị, 014 so "ghi nhận muộn" theo thời điểm thiết bị.
 */
@Component
public class SessionAttendanceOfflineHandler implements OfflineHandler<SessionAttendanceOfflineContent> {
    private final SessionAttendanceService attendance;
    private final ActivitySessionStore store;

    public SessionAttendanceOfflineHandler(SessionAttendanceService attendance, ActivitySessionStore store) {
        this.attendance = attendance;
        this.store = store;
    }

    @Override
    public String recordType() {
        return "M04.SESSION_ATTENDANCE";
    }

    @Override
    public PermissionKey permissionKey() {
        return new PermissionKey(Function.ACTIVITY_OUTING, Operation.CREATE);
    }

    @Override
    public Target target(SessionAttendanceOfflineContent content) {
        var session = store.find(content.sessionId()).orElseThrow(() -> new InvalidValueException("Không có buổi"));
        return new Target(ActivitySessionService.ENTITY, Long.toString(session.id()), null, session.floorAreaId());
    }

    @Override
    public void validate(SessionAttendanceOfflineContent content) {
        items(content).forEach(SessionAttendanceService::validate);
    }

    @Override
    public String apply(SessionAttendanceOfflineContent content, SyncOutcome outcome, Instant effectiveAt) {
        return attendance.apply(content.sessionId(), items(content), effectiveAt).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private static List<SessionAttendanceService.Item> items(SessionAttendanceOfflineContent content) {
        if (content.results() == null || content.results().isEmpty())
            throw new InvalidValueException("Cần ít nhất một kết quả");
        return content.results().stream()
                .map(e -> new SessionAttendanceService.Item(
                        e.residentId(),
                        e.addedAtAttendance(),
                        e.result(),
                        e.absenceReason(),
                        e.participationLevel(),
                        e.communicationLevel(),
                        e.conditionAfter(),
                        e.note()))
                .toList();
    }
}
