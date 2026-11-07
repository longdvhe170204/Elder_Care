package com.eldercare.backend.support.fake.roombedallocation;

import com.eldercare.backend.roombedallocation.api.*;
import com.eldercare.backend.roombedallocation.api.dto.*;
import java.time.Instant;
import java.util.*;

/** 019 sở hữu một fake chung cho API đọc/lệnh lịch xe, mặc định chưa có lịch. */
public class FakeVehicleSchedules implements VehicleScheduleApi, VehicleScheduleCommandApi {
    private final Map<Long, Instant> booked = new HashMap<>();
    private final Set<Long> fail = new HashSet<>();

    public void book(long trip, Instant departure) {
        booked.put(trip, departure);
    }

    public void failCommand(long trip) {
        fail.add(trip);
    }

    @Override
    public boolean hasBookedSchedule(long trip, Instant departure) {
        return departure.equals(booked.get(trip));
    }

    @Override
    public VehicleScheduleChangeCheck checkChange(long trip, Instant from, Instant to) {
        return new VehicleScheduleChangeCheck(true, List.of());
    }

    @Override
    public void followTrip(long trip, TripVehicleChangeType type, Instant from, Instant to, Optional<Instant> actual) {
        if (fail.contains(trip))
            throw new com.eldercare.backend.common.error.BusinessException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "VEHICLE_SOURCE_UNAVAILABLE",
                    "Nguồn lịch xe lỗi");
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        FakeVehicleSchedules fakeVehicleSchedules() {
            return new FakeVehicleSchedules();
        }
    }
}
