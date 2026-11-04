package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import java.util.Set;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Dòng cuối bảng FR-029 (FR-038): người cao tuổi chuyển trạng thái cuối thì mọi cảnh báo đang mở dừng leo thang và nhắc;
 * trạng thái giữ nguyên để người có quyền đóng.
 */
@Component
public class AlertFinalStatusListener {
    private static final Set<ResidentStatus> FINAL =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final AlertStore alerts;
    private final MonitoringLock lock;

    public AlertFinalStatusListener(AlertStore alerts, MonitoringLock lock) {
        this.alerts = alerts;
        this.lock = lock;
    }

    @ApplicationModuleListener
    public void on(ResidentStatusChanged event) {
        if (!FINAL.contains(event.to())) return;
        lock.lock(event.residentId());
        alerts.stopEscalation(event.residentId());
    }
}
