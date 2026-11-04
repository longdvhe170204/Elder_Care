package com.eldercare.backend.health.internal.threshold.service;

import com.eldercare.backend.health.internal.governance.service.VitalSignLock;
import com.eldercare.backend.health.internal.threshold.persistence.ThresholdVersionStore;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import java.util.Set;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Dòng cuối bảng FR-022: người cao tuổi chuyển trạng thái cuối thì ngưỡng cá nhân Hiệu lực hết hiệu lực tại thời điểm
 * chuyển, Chờ hiệu lực bị hủy (tác động hệ thống, không có người và lý do).
 */
@Component
public class ThresholdFinalStatusListener {
    private static final Set<ResidentStatus> FINAL =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final ThresholdVersionStore store;
    private final VitalSignLock lock;

    public ThresholdFinalStatusListener(ThresholdVersionStore store, VitalSignLock lock) {
        this.store = store;
        this.lock = lock;
    }

    @ApplicationModuleListener
    public void on(ResidentStatusChanged event) {
        if (!FINAL.contains(event.to())) return;
        lock.lock(event.residentId());
        for (var version : store.personalOpen(event.residentId())) {
            if ("EFFECTIVE".equals(version.status())) {
                store.end(version.id(), "EXPIRED", event.at(), null, null, version.version());
            } else {
                store.end(version.id(), "CANCELLED", null, null, null, version.version());
            }
        }
    }
}
