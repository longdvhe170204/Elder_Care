package com.eldercare.backend.care.internal.trip.domain;

import java.util.List;

/** Mỗi khoảng lưu đầy đủ phân công; thay đổi đóng khoảng cũ rồi thêm khoảng mới qua StatusHistoryApi. */
public record TripDuties(Long leaderId, List<Long> companionIds) {
    public TripDuties {
        companionIds = List.copyOf(companionIds);
    }
}
