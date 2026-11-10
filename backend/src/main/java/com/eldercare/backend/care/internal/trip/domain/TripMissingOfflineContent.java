package com.eldercare.backend.care.internal.trip.domain;

/** Tham chiếu chuyến cho loại đồng bộ bị chặn vì bắt buộc trực tuyến (FR-046a). */
public record TripMissingOfflineContent(long tripId) {}
