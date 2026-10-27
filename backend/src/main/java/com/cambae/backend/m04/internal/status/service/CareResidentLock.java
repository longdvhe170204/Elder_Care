package com.cambae.backend.m04.internal.status.service;

import com.cambae.backend.m04.internal.status.persistence.CareLockStore;
import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T006: chỉ lệnh đang có transaction mới được tạo/khóa dòng kỹ thuật theo người. */
@Service
public class CareResidentLock {
    private final CareLockStore store;

    public CareResidentLock(CareLockStore store) {
        this.store = store;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(Collection<Long> residentIds) {
        store.lock(residentIds);
    }
}
