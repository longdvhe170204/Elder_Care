package com.cambae.backend.m09.api.spi;

import java.time.Instant;
import java.util.List;

/** Chuyến liên quan ca, nhân viên đang đi cùng và giờ về dự kiến để hiển thị nguồn, không tự lập mục bàn giao. */
public record TripInShiftView(long tripId, List<Long> staffIds, Instant expectedReturnAt) {

    public TripInShiftView {
        staffIds = List.copyOf(staffIds);
    }
}
