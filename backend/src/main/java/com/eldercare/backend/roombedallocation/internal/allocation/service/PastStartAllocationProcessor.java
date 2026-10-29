package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import com.eldercare.backend.roombedallocation.api.events.FutureAllocationPastStart;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.PastStartAllocation;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.PastStartAllocationStore;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Mỗi phân bổ một transaction; kiểm lại M01 và mốc sau khóa trước khi phát báo. */
@Service
public class PastStartAllocationProcessor {
    private final PastStartAllocationStore store;
    private final AllocationLocks locks;
    private final ResidentApi residents;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    public PastStartAllocationProcessor(
            PastStartAllocationStore store,
            AllocationLocks locks,
            ResidentApi residents,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.store = store;
        this.locks = locks;
        this.residents = residents;
        this.time = time;
        this.events = events;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean report(PastStartAllocation pending) {
        locks.lock(pending.residentId(), List.of(pending.bedId()));
        if (residents.status(pending.residentId()) != ResidentStatus.ADMISSION_IN_PROGRESS) return false;
        var current = store.due(time.now()).stream()
                .filter(row -> row.id() == pending.id())
                .findFirst();
        if (current.isEmpty()) return false;
        var row = current.get();
        if (!store.mark(row.id(), row.startAt())) return false;
        events.publishEvent(new FutureAllocationPastStart(
                row.id(),
                row.startAt(),
                new BedNotificationContext(
                        time.now(), row.floorId(), List.of(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)))));
        return true;
    }
}
