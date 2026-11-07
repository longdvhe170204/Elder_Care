package com.eldercare.backend.health.internal.threshold.service;

import com.eldercare.backend.health.internal.governance.service.VitalSignLock;
import com.eldercare.backend.health.internal.threshold.persistence.ThresholdVersionStore;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Dòng cuối bảng FR-022: người cao tuổi chuyển trạng thái cuối thì ngưỡng cá nhân Hiệu lực hết hiệu lực tại thời điểm
 * chuyển, Chờ hiệu lực bị hủy (tác động hệ thống, không có người và lý do). Gọi từ bean tham gia lệnh trạng thái của
 * {@code health} (research S-17).
 */
@Component
public class ThresholdFinalStatus {
    private final ThresholdVersionStore store;
    private final VitalSignLock lock;

    public ThresholdFinalStatus(ThresholdVersionStore store, VitalSignLock lock) {
        this.store = store;
        this.lock = lock;
    }

    public void end(long residentId, Instant at) {
        lock.lock(residentId);
        for (var version : store.personalOpen(residentId)) {
            if ("EFFECTIVE".equals(version.status())) {
                store.end(version.id(), "EXPIRED", at, null, null, version.version());
            } else {
                store.end(version.id(), "CANCELLED", null, null, null, version.version());
            }
        }
    }
}
