package com.eldercare.backend.care.internal.trip.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.api.events.TripChanged;
import com.eldercare.backend.care.internal.session.service.ActivitySessionService;
import com.eldercare.backend.care.internal.trip.domain.TripPlan;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.common.error.InvalidValueException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T017: tạo buổi và phần chuyến trong cùng transaction, giữ nguyên hợp đồng tạo buổi. */
@Service
public class TripCreationService {
    private final ActivitySessionService sessions;
    private final ActivityTripStore trips;
    private final ApplicationEventPublisher events;
    private final com.eldercare.backend.administration.shared.api.SimpleRecordApi records;
    private final com.eldercare.backend.administration.shared.api.CatalogApi catalogs;
    private final com.eldercare.backend.administration.auditlog.api.AuditLogApi audit;

    public TripCreationService(
            ActivitySessionService sessions,
            ActivityTripStore trips,
            ApplicationEventPublisher events,
            com.eldercare.backend.administration.shared.api.SimpleRecordApi records,
            com.eldercare.backend.administration.shared.api.CatalogApi catalogs,
            com.eldercare.backend.administration.auditlog.api.AuditLogApi audit) {
        this.sessions = sessions;
        this.trips = trips;
        this.events = events;
        this.records = records;
        this.catalogs = catalogs;
        this.audit = audit;
    }

    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.CREATE)
    @Transactional
    public long create(
            Target target,
            long activityId,
            Instant startsAt,
            Instant endsAt,
            Long locationId,
            Long responsibleId,
            TripPlan p) {
        var activity = records.require(
                        com.eldercare.backend.care.internal.activity.domain.ActivityKinds.ACTIVITIES, activityId)
                .content();
        var type = catalogs.require(
                com.eldercare.backend.care.internal.activity.domain.ActivityKinds.ACTIVITY_TYPES, activity.typeId());
        if (type.attributes().offSite() != (p != null))
            throw new InvalidValueException(
                    "Hoạt động ngoài viện cần thông tin chuyến; hoạt động trong viện không dùng thông tin chuyến");
        if (p == null) return sessions.createSingle(target, activityId, startsAt, endsAt, locationId, responsibleId);
        if (p.destination() == null
                || p.destination().isBlank()
                || p.transport() == null
                || p.transport().isBlank()
                || p.plannedDepartureAt() == null
                || p.plannedReturnAt() == null
                || !p.plannedReturnAt().isAfter(p.plannedDepartureAt()))
            throw new InvalidValueException("Chuyến cần điểm đến, phương tiện và giờ về sau giờ rời");
        long id = sessions.createSingle(target, activityId, startsAt, endsAt, locationId, responsibleId);
        trips.initialize(id, p);
        audit.write(com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry.builder()
                .action(new com.eldercare.backend.administration.auditlog.api.dto.AuditAction("TRIP_CREATED", false))
                .entity("M04.TRIP", id)
                .before(java.util.Map.of())
                .after(java.util.Map.of(
                        "destination",
                        p.destination(),
                        "plannedDepartureAt",
                        p.plannedDepartureAt().toString(),
                        "plannedReturnAt",
                        p.plannedReturnAt().toString(),
                        "transport",
                        p.transport(),
                        "facilityVehicle",
                        p.facilityVehicle()))
                .build());
        events.publishEvent(new TripChanged(
                id,
                "SCHEDULED",
                p.facilityVehicle(),
                p.plannedDepartureAt(),
                p.plannedReturnAt(),
                Optional.empty(),
                "TRIP_SCHEDULED:" + id));
        return id;
    }
}
