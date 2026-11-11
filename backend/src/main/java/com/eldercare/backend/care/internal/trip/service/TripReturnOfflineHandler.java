package com.eldercare.backend.care.internal.trip.service;

import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.OfflineHandler;
import com.eldercare.backend.care.internal.trip.domain.TripAttendanceOfflineContent;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.offline.SyncOutcome;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * 014 T020 (Q-01, Q24; module-interfaces 014 mục 1.1 {@code NgoaiTuyenHandler} {@code M04.DIEM_DANH_VE}): điểm danh ghi được khi mất mạng;
 * 002 kiểm quyền theo thời điểm thiết bị, thời điểm thiết bị vào bản ghi điểm danh và lịch sử trạng thái 001.
 */
@Component
public class TripReturnOfflineHandler implements OfflineHandler<TripAttendanceOfflineContent> {
    private final TripPreparationService trips;
    private final TripReturnService service;

    public TripReturnOfflineHandler(TripPreparationService trips, TripReturnService service) {
        this.trips = trips;
        this.service = service;
    }

    @Override
    public String recordType() {
        return "M04.TRIP_RETURN";
    }

    @Override
    public PermissionKey permissionKey() {
        return new PermissionKey(Function.ACTIVITY_OUTING, Operation.CREATE);
    }

    @Override
    public Target target(TripAttendanceOfflineContent content) {
        return trips.target(content.tripId());
    }

    @Override
    public void validate(TripAttendanceOfflineContent content) {
        if (content.residentIds() == null || content.residentIds().isEmpty())
            throw new InvalidValueException("Cần danh sách người cao tuổi");
    }

    @Override
    public String apply(TripAttendanceOfflineContent content, SyncOutcome outcome, Instant effectiveAt) {
        service.apply(content.tripId(), content.residentIds(), effectiveAt);
        return Long.toString(content.tripId());
    }
}
