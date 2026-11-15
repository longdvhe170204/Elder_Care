package com.eldercare.backend.care.internal.trip.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.internal.session.service.ActivitySessionService;
import com.eldercare.backend.care.internal.trip.domain.TripPlan;
import com.eldercare.backend.common.error.InvalidValueException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T017: tạo buổi và phần chuyến trong cùng transaction, giữ nguyên hợp đồng tạo buổi. */
@Service
public class TripCreationService {
    private final ActivitySessionService sessions;
    private final TripLifecycle trips;
    private final com.eldercare.backend.administration.shared.api.SimpleRecordApi records;
    private final com.eldercare.backend.administration.shared.api.CatalogApi catalogs;

    public TripCreationService(
            ActivitySessionService sessions,
            TripLifecycle trips,
            com.eldercare.backend.administration.shared.api.SimpleRecordApi records,
            com.eldercare.backend.administration.shared.api.CatalogApi catalogs) {
        this.sessions = sessions;
        this.trips = trips;
        this.records = records;
        this.catalogs = catalogs;
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
        p.validate();
        long id = sessions.createSingle(target, activityId, startsAt, endsAt, locationId, responsibleId);
        trips.schedule(id, p);
        return id;
    }
}
