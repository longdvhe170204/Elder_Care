package com.cambae.backend.m09.duty.internal.service;

import com.cambae.backend.m03.api.dto.*;
import com.cambae.backend.m03.api.spi.LocationReferences;
import java.util.*;
import org.springframework.stereotype.Service;

/** T015: tham chiếu ca; phần phân công/giao Trưởng tầng nối cùng bảng T019. */
@Service
class ShiftLocationReferences implements LocationReferences {
    private final ShiftQueries query;

    ShiftLocationReferences(ShiftQueries query) {
        this.query = query;
    }

    public List<LocationReferenceView> activeReferences(LocationRef location) {
        return query.locationReferences(location.type(), location.id(), true).stream()
                .map(s -> new LocationReferenceView("M09.SHIFT", s.id()))
                .toList();
    }

    public boolean everReferenced(LocationRef location) {
        return !query.locationReferences(location.type(), location.id(), false).isEmpty();
    }
}
