package com.eldercare.backend.care.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** 014 ActivityStatisticsApi.trips: giờ về dự kiến ban đầu, các lần gia hạn, về thực tế, số người thiếu khi về. */
public record TripStatisticsView(
        long tripId,
        Optional<Long> floorAreaId,
        Instant plannedReturnAt,
        List<Instant> extendedReturnAt,
        Optional<Instant> actualReturnAt,
        int missingOnReturn) {}
