package com.eldercare.backend.health.internal.schedule.service;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import java.util.Set;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Dòng cuối bảng FR-006: người cao tuổi chuyển trạng thái cuối thì lịch đo đang mở Đã ngừng. */
@Component
public class MeasurementScheduleFinalStatusListener {
    private static final Set<ResidentStatus> FINAL =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final MeasurementScheduleService schedules;

    public MeasurementScheduleFinalStatusListener(MeasurementScheduleService schedules) {
        this.schedules = schedules;
    }

    @ApplicationModuleListener
    public void on(ResidentStatusChanged event) {
        if (FINAL.contains(event.to())) schedules.stopForFinalStatus(event.residentId(), event.at());
    }
}
