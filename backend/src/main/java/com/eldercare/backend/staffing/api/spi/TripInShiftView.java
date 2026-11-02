package com.eldercare.backend.staffing.api.spi;

import java.time.Instant;
import java.util.List;

/** Chuyến liên quan ca, nhân viên đang đi cùng và giờ về dự kiến để hiển thị nguồn, không tự lập mục bàn giao. */
public record TripInShiftView(
        long tripId, List<Long> staffIds, Instant expectedReturnAt, List<Long> residentIds, Instant departedAt) {

    public TripInShiftView {
        staffIds = List.copyOf(staffIds);
        residentIds = List.copyOf(residentIds);
    }

    public TripInShiftView(long tripId, List<Long> staffIds, Instant expectedReturnAt) {
        this(tripId, staffIds, expectedReturnAt, List.of(), null);
    }
}
