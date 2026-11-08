package com.eldercare.backend.care.internal.session.service;

import static com.eldercare.backend.care.internal.activity.domain.ActivityKinds.*;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.care.api.ActivityRegistrationAutoCancelApi;
import com.eldercare.backend.care.api.spi.ActivityHealthGatePort;
import com.eldercare.backend.care.internal.activity.domain.RegularParticipant;
import com.eldercare.backend.care.internal.session.domain.Registration;
import com.eldercare.backend.care.internal.session.domain.Session;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T011 (FR-021, FR-021a, FR-022): hủy tự động trong cùng transaction của lệnh gây ra (007 khoanh vùng, gắn dấu nghi
 * nhiễm; 001 chuyển trạng thái cuối). Không khôi phục khi vùng được gỡ hay dấu được gỡ.
 */
@Service
public class RegistrationAutoCancels implements ActivityRegistrationAutoCancelApi {
    private final ActivitySessionStore store;
    private final ActivitySessionService sessions;
    private final SimpleRecordApi records;
    private final RegistrationNotices notices;
    private final ObjectProvider<ActivityHealthGatePort> gates;
    private final BusinessTime time;
    private final com.eldercare.backend.care.internal.trip.service.TripLifecycle trips;

    public RegistrationAutoCancels(
            ActivitySessionStore store,
            ActivitySessionService sessions,
            SimpleRecordApi records,
            RegistrationNotices notices,
            ObjectProvider<ActivityHealthGatePort> gates,
            BusinessTime time,
            com.eldercare.backend.care.internal.trip.service.TripLifecycle trips) {
        this.store = store;
        this.sessions = sessions;
        this.records = records;
        this.notices = notices;
        this.gates = gates;
        this.time = time;
        this.trips = trips;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelForIsolation(long isolationZoneId, Instant at) {
        var gate = gates.getObject();
        String reason = "Khoanh vùng";
        for (var s : store.scheduledAfter(at))
            if (gate.locationIsolated(s.locationRoomId(), s.locationAreaId(), s.floorAreaId(), at)
                    && store.activeRegistrations(s.id()).stream()
                            .noneMatch(r -> Registration.ATTENDED.equals(r.status())))
                sessions.cancelSession(s, Session.BY_ISOLATION, reason, null);
        cancelEach(
                store.registeredAfter(at, null).stream()
                        .filter(r -> gate.residentIsolated(r.residentId(), at))
                        .toList(),
                Registration.AUTO_ISOLATION,
                reason,
                at);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelForSuspectedInfection(long residentId, Instant at) {
        trips.suspectedInfection(residentId, at);
        cancelEach(
                store.registeredAfter(at, residentId).stream()
                        .filter(r -> groupActivity(r.sessionId()))
                        .toList(),
                Registration.AUTO_INFECTION,
                "Nghi nhiễm",
                at);
    }

    /** FR-022: trạng thái cuối hủy đăng ký chưa diễn ra, gỡ khỏi mọi danh sách người thường xuyên. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelForFinalStatus(long residentId, Instant at) {
        cancelEach(store.registeredAfter(at, residentId), Registration.AUTO_FINAL_STATUS, "Trạng thái cuối", at);
        for (var row : records.listByResident(REGULAR_PARTICIPANTS, residentId, Set.of(ACTIVE)))
            records.change(
                    REGULAR_PARTICIPANTS,
                    row.id(),
                    row.version(),
                    new RecordChange<>(
                            ENDED,
                            new RegularParticipant(row.content().activityId(), "Trạng thái cuối"),
                            LocalDate.ofInstant(at, BusinessTime.ZONE),
                            null,
                            false));
    }

    private boolean groupActivity(long sessionId) {
        var session = store.find(sessionId).orElseThrow();
        return session.tripStatus() != null
                || records.require(ACTIVITIES, session.activityId()).content().groupActivity();
    }

    private void cancelEach(List<Registration> registrations, String auto, String reason, Instant at) {
        var bySession = new LinkedHashMap<Long, List<Registration>>();
        for (var r : registrations) {
            store.cancelRegistration(r.id(), auto, reason);
            trips.registrationChanged(r.sessionId(), r.residentId(), true);
            bySession.computeIfAbsent(r.sessionId(), k -> new ArrayList<>()).add(r);
        }
        bySession.forEach(
                (sessionId, list) -> notices.cancelled(store.find(sessionId).orElseThrow(), list, auto, time.now()));
    }
}
