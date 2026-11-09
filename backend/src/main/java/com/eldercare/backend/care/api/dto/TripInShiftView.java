package com.eldercare.backend.care.api.dto;

import java.time.Instant;
import java.util.List;

/** 014 cung cấp nguồn chuyến cho adapter 008, giữ năm trường đã công bố của 008. */
public record TripInShiftView(
        long tripId, List<Long> staffIds, Instant expectedReturnAt, List<Long> residentIds, Instant departedAt) {
    public TripInShiftView {
        staffIds = List.copyOf(staffIds);
        residentIds = List.copyOf(residentIds);
    }
}
