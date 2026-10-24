package com.cambae.backend.m03.internal.allocation.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.*;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.dto.BedNotificationContext;
import com.cambae.backend.m03.api.events.AllocationNoLongerSuitable;
import com.cambae.backend.m03.internal.allocation.domain.AllocationStatus;
import com.cambae.backend.m03.internal.allocation.persistence.*;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T035: luôn tính theo dữ liệu hiện hành; sự kiện đến lại hoặc sai thứ tự không đặt dấu theo payload cũ. */
@Service
public class AllocationSuitabilityService {
    private final AllocationReadStore read;
    private final AllocationCommandStore write;
    private final AllocationLocks locks;
    private final AllocationChecker checker;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    public AllocationSuitabilityService(
            AllocationReadStore read,
            AllocationCommandStore write,
            AllocationLocks locks,
            AllocationChecker checker,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.read = read;
        this.write = write;
        this.locks = locks;
        this.checker = checker;
        this.time = time;
        this.events = events;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void reassess(long residentId) {
        locks.lock(residentId, List.of());
        var rows = read.list(residentId, null, null).stream()
                .filter(row -> row.status() == AllocationStatus.ACTIVE || row.status() == AllocationStatus.FUTURE)
                .toList();
        locks.lock(residentId, rows.stream().map(row -> row.location().bedId()).toList());
        for (var row : rows) {
            var conditions = checker.policyConditions(residentId, row.location().bedId());
            boolean unsuitable = !conditions.isEmpty();
            if (write.markUnsuitable(row.id(), unsuitable) && unsuitable)
                events.publishEvent(new AllocationNoLongerSuitable(
                        row.id(),
                        conditions,
                        new BedNotificationContext(
                                time.now(),
                                row.location().floorId(),
                                List.of(
                                        new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                                        new RecipientGroup.FloorManager(
                                                row.location().floorId(), new Substitution.Default())))));
        }
    }
}
