package com.eldercare.backend.care.internal.trip.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.TripSafetyNotice;
import com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T020 (FR-049, FR-049a; BR-M04-17): quá giờ về dự kiến + CFG-M04-07 thì báo trưởng đoàn và Trưởng tầng, quá hai lần
 * thì báo Quản lý viện; mỗi mốc giờ về một lần ({@code trip_overdue_notified_for}, {@code trip_manager_notified_for}).
 * Gia hạn đổi mốc nên mốc cũ không còn báo. Không đổi trạng thái người tham gia.
 */
@Component
public class TripReturnOverdueStep implements CareMinuteWork {
    static final String OVERDUE = "RETURN_OVERDUE";
    static final String MANAGER = "RETURN_OVERDUE_MANAGER";

    private final ActivityTripStore trips;
    private final TripLifecycle lifecycle;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;

    public TripReturnOverdueStep(
            ActivityTripStore trips,
            TripLifecycle lifecycle,
            ParameterApi parameters,
            ApplicationEventPublisher events) {
        this.trips = trips;
        this.lifecycle = lifecycle;
        this.parameters = parameters;
        this.events = events;
    }

    @Override
    @Transactional
    public int run(Instant now) {
        var threshold = parameters.read(ParameterCode.CFG_M04_07).asDuration();
        int count = 0;
        for (var trip : trips.overdue(now.minus(threshold), false)) {
            var milestone = trip.plan().plannedReturnAt();
            if (!trips.claimOverdue(trip.id(), milestone, false)) continue;
            lifecycle.safetyNotice(trip, OVERDUE, OVERDUE + ":" + trip.id() + ":" + milestone, now);
            count++;
        }
        for (var trip : trips.overdue(now.minus(threshold.multipliedBy(2)), true)) {
            var milestone = trip.plan().plannedReturnAt();
            if (!trips.claimOverdue(trip.id(), milestone, true)) continue;
            events.publishEvent(new TripSafetyNotice(
                    trip.id(),
                    MANAGER,
                    new CareEventContext(
                            "M04.TRIP",
                            trip.id(),
                            MANAGER + ":" + trip.id() + ":" + milestone,
                            now,
                            Optional.ofNullable(trip.floorAreaId()),
                            List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)))));
            count++;
        }
        return count;
    }
}
