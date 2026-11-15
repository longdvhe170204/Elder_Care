package com.eldercare.backend.care.internal.attendance.service;

import com.eldercare.backend.admission.api.events.AddendumApplied;
import com.eldercare.backend.admission.api.events.DayCareAbsenceNoticeRecorded;
import com.eldercare.backend.admission.api.events.DayCareClosureDayChanged;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * T029: báo vắng, ngày nghỉ của 004 xác định lại trạng thái có mặt ngay trong lệnh của 004 (listener đồng bộ, lỗi ở đây
 * làm lệnh 004 rollback; module-interfaces 004 mục 1.6a). Ngày sau hôm nay thì việc đầu ngày đọc lịch khi tới ngày.
 * Phụ lục đổi lịch đến ({@code ARRIVAL_SCHEDULE}, glossary 10.3 {@code LICH_DEN}) xác định lại các ngày từ ngày hiệu lực
 * tới hôm nay theo lịch mới (FR-012; module-interfaces 005 "Sự kiện nhận"). Ngày bị bỏ khỏi lịch mới không xử lý ở lượt
 * này (chờ Q mới, chủ dự án chọn 2026-11-09).
 */
@Component
class AttendanceCalendarListener {
    private final AttendanceSources sources;
    private final AttendanceService commands;
    private final AttendanceDayStartWork dayStart;
    private final BusinessTime time;
    private final ReminderMarkers markers;

    AttendanceCalendarListener(
            AttendanceSources sources,
            AttendanceService commands,
            AttendanceDayStartWork dayStart,
            BusinessTime time,
            ReminderMarkers markers) {
        this.sources = sources;
        this.commands = commands;
        this.dayStart = dayStart;
        this.time = time;
        this.markers = markers;
    }

    @EventListener
    void noticeRecorded(DayCareAbsenceNoticeRecorded event) {
        if (event.date().isAfter(time.today()) || !sources.staying(event.residentId(), event.date())) return;
        commands.refresh(event.residentId(), event.date());
    }

    @EventListener
    void addendumApplied(AddendumApplied event) {
        if (!event.changeTypes().contains("ARRIVAL_SCHEDULE")) return;
        LocalDate today = time.today();
        if (event.effectiveDate().isAfter(today)) return;
        if (!markers.mark(
                "M04.ADDENDUM_ATTENDANCE",
                "M02.ADDENDUM",
                event.addendumId(),
                "APPLIED",
                Map.of("effectiveDate", event.effectiveDate().toString()))) return;
        for (LocalDate day = event.effectiveDate(); !day.isAfter(today); day = day.plusDays(1)) {
            if (!sources.staying(event.residentId(), day)) continue;
            // Chỉ ngày còn buổi theo lịch mới: thêm ngày, đổi giờ (FR-012); ngày bị bỏ khỏi lịch chờ Q mới
            if (sources.day(event.residentId(), day).arrival() == null) continue;
            commands.refresh(event.residentId(), day);
        }
    }

    @EventListener
    void closureChanged(DayCareClosureDayChanged event) {
        dayStart.run(event.date(), time.now());
    }
}
