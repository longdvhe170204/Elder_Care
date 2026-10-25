package com.cambae.backend.m03.internal.bed.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.dto.BedNotificationContext;
import com.cambae.backend.m03.api.events.*;
import com.cambae.backend.m03.api.spi.AllocationResidencySource;
import com.cambae.backend.m03.internal.allocation.service.AllocationLocks;
import com.cambae.backend.m03.internal.bed.domain.BedEvent;
import com.cambae.backend.m03.internal.bed.persistence.BedHoldStore;
import com.cambae.backend.m15.auditlog.api.*;
import com.cambae.backend.m15.parameter.api.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T044: mỗi giường một transaction, kiểm lại sau khóa và không giải phóng người vắng theo đồng hồ. */
@Service
public class HoldExpiryProcessor {
    private final BedHoldStore store;
    private final AllocationLocks locks;
    private final BedStateService state;
    private final BedHoldService holds;
    private final BusinessTime time;
    private final ObjectProvider<AllocationResidencySource> residency;
    private final ApplicationEventPublisher events;
    private final AuditLogApi audit;
    private final ParameterApi parameters;

    public HoldExpiryProcessor(
            BedHoldStore store,
            AllocationLocks locks,
            BedStateService state,
            BedHoldService holds,
            BusinessTime time,
            ObjectProvider<AllocationResidencySource> residency,
            ApplicationEventPublisher events,
            AuditLogApi audit,
            ParameterApi parameters) {
        this.store = store;
        this.locks = locks;
        this.state = state;
        this.holds = holds;
        this.time = time;
        this.residency = residency;
        this.events = events;
        this.audit = audit;
        this.parameters = parameters;
    }

    @Transactional
    public boolean expire(BedHoldStore.Held held, long runId) {
        locks.lock(held.residentId(), List.of(held.bedId()));
        var now = time.now();
        if (!store.stillExpired(held.bedId(), now)) return false;
        // Đọc lại hạn sau khóa, không dùng hạn từ danh sách quét trước transaction.
        long bedId = held.bedId();
        held = store.expired(now).stream()
                .filter(row -> row.bedId() == bedId)
                .findFirst()
                .orElseThrow();
        var basis = new Basis("FR-015", null, null, runId);
        if (held.reason().equals("WAITING_RECORD")) {
            var source = residency.getIfAvailable();
            if (source == null)
                throw new BusinessException(
                        HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn hồ sơ chờ 004");
            state.transition(
                    held.bedId(),
                    holds.command(
                            BedEvent.HOLD_EXPIRED,
                            null,
                            basis,
                            "WAITING_RECORD",
                            held.waitingId(),
                            held.until(),
                            null,
                            null));
            source.waitingRecordHoldExpired(held.waitingId());
            holds.becameEmpty(held.bedId(), "HOLD_EXPIRED");
        } else {
            boolean late = Duration.between(held.until(), now)
                            .compareTo(parameters.read(ParameterCode.CFG_M03_08).asDuration())
                    > 0;
            store.markExpiry(held.bedId(), now);
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("M03_ABSENT_HOLD_EXPIRED", false))
                    .entity("M03.BED", held.bedId())
                    .basis(basis)
                    .details(Map.of("plannedAt", held.until().toString(), "executedLate", late))
                    .build());
            events.publishEvent(new AbsentResidentHoldExpired(held.bedId(), held.residentId()));
            if (late)
                events.publishEvent(new DelayedScheduledWork(
                        "ABSENT_HOLD",
                        held.bedId(),
                        held.until(),
                        now,
                        new BedNotificationContext(
                                now, null, List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)))));
        }
        return true;
    }
}
