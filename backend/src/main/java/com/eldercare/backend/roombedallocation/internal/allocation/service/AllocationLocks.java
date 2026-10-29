package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationStore;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T025/G-03: sau khóa M01 (nếu lệnh do 001 gọi), khóa người rồi giường tăng dần trong transaction nguồn. */
@Service
public class AllocationLocks {
    private final AllocationStore store;

    public AllocationLocks(AllocationStore store) {
        this.store = store;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(long residentId, List<Long> bedIds) {
        store.guardWrites(Integer.valueOf(java.sql.Connection.TRANSACTION_READ_COMMITTED)
                .equals(org.springframework.transaction.support.TransactionSynchronizationManager
                        .getCurrentTransactionIsolationLevel()));
        store.lockPosition(residentId);
        store.guardRooms(bedIds);
        bedIds.stream().distinct().sorted().forEach(store::lockBed);
    }
}
