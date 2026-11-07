package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.roombedallocation.api.dto.VehicleScheduleChangeCheck;
import java.time.Instant;

/** 019 contracts mục 2.2, 014 đọc; thiếu provider thật thì chuyến xe viện chưa được rời viện. */
public interface VehicleScheduleApi {
    boolean hasBookedSchedule(long tripId, Instant plannedDepartureAt);

    VehicleScheduleChangeCheck checkChange(long tripId, Instant newDepartureAt, Instant newReturnAt);
}
