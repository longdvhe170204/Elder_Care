package com.eldercare.backend.care.internal.attendance.service;

import com.eldercare.backend.admission.api.events.DayCareAbsenceNoticeRecorded;
import com.eldercare.backend.admission.api.events.DayCareClosureDayChanged;
import com.eldercare.backend.common.time.BusinessTime;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * T029: báo vắng, ngày nghỉ của 004 xác định lại trạng thái có mặt ngay trong lệnh của 004 (listener đồng bộ, lỗi ở đây
 * làm lệnh 004 rollback; module-interfaces 004 mục 1.6a). Ngày sau hôm nay thì việc đầu ngày đọc lịch khi tới ngày.
 */
@Component
class AttendanceCalendarListener {
    private final AttendanceSources sources;
    private final AttendanceService commands;
    private final AttendanceDayStartWork dayStart;
    private final BusinessTime time;

    AttendanceCalendarListener(
            AttendanceSources sources, AttendanceService commands, AttendanceDayStartWork dayStart, BusinessTime time) {
        this.sources = sources;
        this.commands = commands;
        this.dayStart = dayStart;
        this.time = time;
    }

    @EventListener
    void noticeRecorded(DayCareAbsenceNoticeRecorded event) {
        if (event.date().isAfter(time.today()) || !sources.staying(event.residentId(), event.date())) return;
        commands.refresh(event.residentId(), event.date());
    }

    @EventListener
    void closureChanged(DayCareClosureDayChanged event) {
        dayStart.run(event.date(), time.now());
    }
}
