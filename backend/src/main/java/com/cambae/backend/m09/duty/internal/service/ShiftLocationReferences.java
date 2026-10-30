package com.cambae.backend.m09.duty.internal.service;

import com.cambae.backend.m03.api.dto.*;
import com.cambae.backend.m03.api.spi.LocationReferenceView;
import com.cambae.backend.m03.api.spi.LocationReferences;
import java.util.*;
import org.springframework.stereotype.Service;

/** T015: tham chiếu ca; phần phân công/giao Trưởng tầng nối cùng bảng T019. */
@Service
class ShiftLocationReferences implements LocationReferences {
    private final ShiftQueries query;
    private final com.cambae.backend.m09.assignment.internal.persistence.AssignmentStore assignments;
    private final com.cambae.backend.common.time.BusinessTime time;

    ShiftLocationReferences(
            ShiftQueries query,
            com.cambae.backend.m09.assignment.internal.persistence.AssignmentStore assignments,
            com.cambae.backend.common.time.BusinessTime time) {
        this.query = query;
        this.assignments = assignments;
        this.time = time;
    }

    public List<LocationReferenceView> currentReferences(LocationRef location) {
        var refs = new ArrayList<LocationReferenceView>();
        query.locationReferences(location.type(), location.id(), true)
                .forEach(s -> refs.add(new LocationReferenceView("M09.SHIFT", s.id(), "M09.SHIFT")));
        assignments
                .references(location.type(), location.id(), true)
                .forEach(id -> refs.add(new LocationReferenceView("M09.ASSIGNMENT", id, "Phân công #" + id)));
        if (Set.of("FLOOR", "AREA").contains(location.type()))
            assignments.managers(location.id(), false).stream()
                    .filter(a -> "ACTIVE".equals(a.status())
                            && (a.endsAt() == null || a.endsAt().isAfter(time.now())))
                    .forEach(a -> refs.add(new LocationReferenceView(
                            "M09.FLOOR_MANAGER_ASSIGNMENT", a.id(), "M09.FLOOR_MANAGER_ASSIGNMENT")));
        return List.copyOf(refs);
    }

    public boolean everReferenced(LocationRef location) {
        return !query.locationReferences(location.type(), location.id(), false).isEmpty()
                || !assignments
                        .references(location.type(), location.id(), false)
                        .isEmpty()
                || Set.of("FLOOR", "AREA").contains(location.type())
                        && !assignments.managers(location.id(), false).isEmpty();
    }
}
