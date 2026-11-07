package com.eldercare.backend.care.internal.session.service;

import static com.eldercare.backend.care.internal.activity.domain.ActivityKinds.ACTIVITIES;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.care.api.spi.TaskSourcePort;
import com.eldercare.backend.care.internal.session.domain.Registration;
import com.eldercare.backend.care.internal.session.domain.Session;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * 014 T014 (module-interfaces 014 mục 1.1 {@code NguonCongViecPort} {@code BUOI_HOAT_DONG}; 005 FR-017): đăng ký còn hiệu
 * lực là căn cứ để Người phụ trách ca lập công việc "hoạt động"; 014 chỉ trả căn cứ, không lập công việc (Q-306). Làm lại
 * ({@code REWORK}) thêm ở US7.
 */
@Component
public class ActivityTaskSource implements TaskSourcePort {
    private final ActivitySessionStore store;
    private final SimpleRecordApi records;

    public ActivityTaskSource(ActivitySessionStore store, SimpleRecordApi records) {
        this.store = store;
        this.records = records;
    }

    @Override
    public Set<SourceType> managedTypes() {
        return EnumSet.of(SourceType.ACTIVITY_SESSION);
    }

    @Override
    public SourceValidation validate(SourceType type, long sourceId, long residentId, Instant plannedAt) {
        var registration = store.findRegistration(sourceId).orElse(null);
        if (type != SourceType.ACTIVITY_SESSION || registration == null)
            return new SourceValidation(false, false, false, false);
        var session = store.find(registration.sessionId()).orElseThrow();
        boolean valid =
                !Registration.CANCELLED.equals(registration.status()) && Session.SCHEDULED.equals(session.status());
        boolean time = !plannedAt.isBefore(session.startsAt()) && !plannedAt.isAfter(session.endsAt());
        return new SourceValidation(valid, registration.residentId() == residentId, time, true);
    }

    @Override
    public List<SourceView> sources(long residentId, Instant from, Instant to) {
        return store.registrationsInWindow(residentId, from, to).stream()
                .map(r -> {
                    var session = store.find(r.sessionId()).orElseThrow();
                    var name = records.require(ACTIVITIES, session.activityId())
                            .content()
                            .name();
                    return new SourceView(SourceType.ACTIVITY_SESSION, r.id(), session.startsAt(), name);
                })
                .toList();
    }
}
