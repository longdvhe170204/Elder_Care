package com.eldercare.backend.health.internal.governance.service;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.health.internal.governance.domain.ResidentVitalSignState;
import com.eldercare.backend.health.internal.governance.persistence.VitalSignStateMutex;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Khóa M06 trước M05 theo S-03; chỉ tạo trạng thái khi có lệnh trong transaction. */
@Service
public class VitalSignLock {
    private final VitalSignStateMutex mutex;
    private final ResidentModuleStateApi states;

    public VitalSignLock(VitalSignStateMutex mutex, ResidentModuleStateApi states) {
        this.mutex = mutex;
        this.states = states;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(long residentId) {
        String key = ResidentVitalSignState.KIND.code() + ":" + residentId;
        mutex.acquire(key);
        try {
            if (states.lockForUpdate(ResidentVitalSignState.KIND, residentId).isEmpty()) {
                states.save(ResidentVitalSignState.KIND, residentId, new ResidentVitalSignState(null));
            }
        } finally {
            mutex.release(key);
        }
    }
}
