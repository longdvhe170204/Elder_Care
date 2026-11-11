package com.eldercare.backend.care.internal.trip.domain;

import java.util.List;

/**
 * 014 T020 (Q-01): nội dung bản ghi ngoại tuyến {@code M04.TRIP_DEPARTURE}, {@code M04.TRIP_RETURN}, cùng trường với thân
 * lệnh {@code depart}, {@code return} của {@code TripCommandWrite}; {@code reason} là lý do khi người thay trưởng đoàn điểm
 * danh rời viện.
 */
public record TripAttendanceOfflineContent(long tripId, List<Long> residentIds, String reason) {}
