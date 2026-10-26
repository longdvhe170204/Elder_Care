package com.cambae.backend.m04.api.spi;

import java.time.*;
import java.util.List;

/** 006, 012, 014 mỗi bên một bean chỉ đọc (FR-048). */
public interface TimetableSourcePort {
    List<TimetableItemView> onDate(long residentId, LocalDate date);

    record TimetableItemView(Instant at, String source, String description, String status) {}
}
