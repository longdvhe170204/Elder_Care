package com.eldercare.backend.care.internal.attendance.service;

import com.eldercare.backend.admission.api.dto.Attendance;
import com.eldercare.backend.admission.api.spi.DayCareAttendancePort;
import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.care.internal.attendance.persistence.AttendanceStore;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * T029: {@code TrangThaiCoMatBanTruPort} của 004 ({@link DayCareAttendancePort}, FR-060; module-interfaces 004 mục 2.1),
 * 004 đọc khi tính hệ số buổi ({@code DayCareSessions}). 005 giữ một khung lịch đến mỗi ngày ({@link AttendanceSources#day}
 * trả 503 khi nhiều khung), nên chỉ buổi {@code sessionId = 1} (buổi đầu theo giờ đến, cách 004 đánh số) có trạng thái.
 * Có mặt gồm cả đã về (đến muộn, về sớm vẫn 100%); Chưa đến, ngày nghỉ là chưa xác định (rỗng).
 */
@Component
class DayCareAttendanceStatuses implements DayCareAttendancePort {
    private final AttendanceStore store;

    DayCareAttendanceStatuses(AttendanceStore store) {
        this.store = store;
    }

    @Override
    public Optional<Attendance> status(long residentId, LocalDate date, long sessionId) {
        if (sessionId != 1) return Optional.empty();
        return store.find(residentId, date).flatMap(row -> switch (AttendanceStatus.valueOf(row.status())) {
            case PRESENT, LEFT -> Optional.of(Attendance.PRESENT);
            case REPORTED_ABSENT -> Optional.of(Attendance.ABSENT_NOTIFIED);
            case UNREPORTED_ABSENT -> Optional.of(Attendance.ABSENT_UNNOTIFIED);
            case NOT_ARRIVED, HOLIDAY_CANCELLED -> Optional.<Attendance>empty();
        });
    }
}
