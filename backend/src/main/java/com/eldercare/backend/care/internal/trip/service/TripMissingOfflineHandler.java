package com.eldercare.backend.care.internal.trip.service;

import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.OfflineHandler;
import com.eldercare.backend.care.internal.trip.domain.TripMissingOfflineContent;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.offline.SyncOutcome;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** FR-046a: bộ đồng bộ chung từ chối trước khi giải nội dung hay gọi nguồn sự cố. */
@Component
public class TripMissingOfflineHandler implements OfflineHandler<TripMissingOfflineContent> {
    private final TripPreparationService trips;

    public TripMissingOfflineHandler(TripPreparationService trips) {
        this.trips = trips;
    }

    @Override
    public String recordType() {
        return "M04.TRIP_MISSING";
    }

    @Override
    public PermissionKey permissionKey() {
        return new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.CREATE);
    }

    @Override
    public Target target(TripMissingOfflineContent content) {
        return trips.target(content.tripId());
    }

    @Override
    public boolean requiresOnline() {
        return true;
    }

    @Override
    public void validate(TripMissingOfflineContent content) {
        throw onlineRequired();
    }

    @Override
    public String apply(TripMissingOfflineContent content, SyncOutcome outcome, Instant effectiveAt) {
        throw onlineRequired();
    }

    private static BusinessException onlineRequired() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "REQUIRES_ONLINE",
                "Báo thiếu người chỉ thực hiện trực tuyến, gọi Trưởng tầng khi mất mạng");
    }
}
