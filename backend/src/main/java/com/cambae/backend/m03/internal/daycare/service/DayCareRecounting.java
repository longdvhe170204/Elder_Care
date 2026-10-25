package com.cambae.backend.m03.internal.daycare.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.*;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.dto.*;
import com.cambae.backend.m03.api.events.DayCareOverCapacity;
import com.cambae.backend.m03.api.spi.DayCareArrivalCounter;
import com.cambae.backend.m03.internal.daycare.domain.SessionCapacityRow;
import com.cambae.backend.m03.internal.daycare.persistence.DayCareStore;
import com.cambae.backend.m15.auditlog.api.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Đếm từ nguồn 004; chỉ cảnh báo và ghi nhật ký, không thay đổi lịch đến. */
@Service
public class DayCareRecounting {
    private final DayCareStore store;
    private final DayCareCapacityService capacity;
    private final ObjectProvider<DayCareArrivalCounter> arrivals;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;

    public DayCareRecounting(
            DayCareStore store,
            DayCareCapacityService capacity,
            ObjectProvider<DayCareArrivalCounter> arrivals,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events) {
        this.store = store;
        this.capacity = capacity;
        this.arrivals = arrivals;
        this.time = time;
        this.audit = audit;
        this.events = events;
    }

    public DayCareArrivalCounter source() {
        var source = arrivals.getIfAvailable();
        if (source == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn lịch đến 004");
        return source;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<SessionCapacityRow> recount() {
        store.lock();
        if (store.activeArea(true).isEmpty()) return List.of();
        var sessions = store.sessions(true).stream()
                .map(x -> x.view())
                .filter(DayCareSession::active)
                .toList();
        var counts = source().countFrom(time.today(), sessions);
        if (counts.isEmpty()) return List.of();
        int limit = capacity.capacity();
        var rows = rows(counts, sessions, limit).stream()
                .filter(x -> x.remaining() < 0)
                .toList();
        if (!rows.isEmpty()) {
            events.publishEvent(new DayCareOverCapacity(
                    rows.stream()
                            .map(x -> new SessionDate(x.date(), x.sessionId()))
                            .toList(),
                    new BedNotificationContext(
                            time.now(), null, List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)))));
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("M03_DAY_CARE_OVER_CAPACITY", false))
                    .basis(new Basis("BR-M03-25", null, null, null))
                    .entity(
                            "DAY_CARE_REST_AREA",
                            store.activeArea(true).orElseThrow().id())
                    .details(Map.of("sessions", rows))
                    .build());
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<SessionCapacityRow> counts(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from))
            throw new InvalidValueException("Khoảng ngày không hợp lệ");
        var sessions = store.sessions(false).stream()
                .map(x -> x.view())
                .filter(DayCareSession::active)
                .toList();
        int limit = capacity.capacity();
        var registered = source().countFrom(from, sessions);
        var all = new LinkedHashMap<SessionDate, Integer>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1))
            for (var session : sessions) {
                var key = new SessionDate(day, session.id());
                all.put(key, registered.getOrDefault(key, 0));
            }
        return rows(all, sessions, limit);
    }

    private List<SessionCapacityRow> rows(Map<SessionDate, Integer> counts, List<DayCareSession> sessions, int limit) {
        return counts.entrySet().stream()
                .filter(x ->
                        sessions.stream().anyMatch(s -> s.id() == x.getKey().sessionId()))
                .sorted(Map.Entry.comparingByKey(
                        Comparator.comparing(SessionDate::date).thenComparingLong(SessionDate::sessionId)))
                .map(x -> new SessionCapacityRow(
                        x.getKey().date(),
                        x.getKey().sessionId(),
                        sessions.stream()
                                .filter(s -> s.id() == x.getKey().sessionId())
                                .findFirst()
                                .orElseThrow()
                                .name(),
                        x.getValue(),
                        limit,
                        limit - x.getValue()))
                .toList();
    }
}
