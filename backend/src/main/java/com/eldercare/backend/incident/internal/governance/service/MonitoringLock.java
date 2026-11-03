package com.eldercare.backend.incident.internal.governance.service;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.incident.internal.governance.domain.ResidentMonitoringState;
import com.eldercare.backend.incident.internal.governance.persistence.MonitoringStateMutex;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Khóa M05 theo người trước khi khóa dòng nghiệp vụ (S-03, T007). */
@Service
public class MonitoringLock {
    private final MonitoringStateMutex mutex;
    private final ResidentModuleStateApi states;

    public MonitoringLock(MonitoringStateMutex mutex, ResidentModuleStateApi states) {
        this.mutex = mutex;
        this.states = states;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(long residentId) {
        String key = ResidentMonitoringState.KIND.code() + ":" + residentId;
        mutex.acquire(key);
        try {
            if (states.lockForUpdate(ResidentMonitoringState.KIND, residentId).isEmpty()) {
                states.save(ResidentMonitoringState.KIND, residentId, new ResidentMonitoringState());
            }
        } finally {
            mutex.release(key);
        }
    }
}
