package com.cambae.backend.support.fake.m03;

import com.cambae.backend.m03.api.dto.LocationRef;
import com.cambae.backend.m03.api.spi.LocationReferenceView;
import com.cambae.backend.m03.api.spi.LocationReferences;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bản giả tham chiếu 008 (FR-005): tách tham chiếu hiện tại và dấu đã từng tham chiếu. */
public class FakeLocationReferences implements LocationReferences {
    private final Map<LocationRef, List<LocationReferenceView>> references = new HashMap<>();
    private final Set<LocationRef> historical = new HashSet<>();

    public FakeLocationReferences references(
            LocationRef location, List<com.cambae.backend.m03.api.dto.LocationReferenceView> rows) {
        references.put(
                location,
                rows.stream()
                        .map(row -> new LocationReferenceView(row.type(), row.id(), row.type()))
                        .toList());
        if (!rows.isEmpty()) historical.add(location);
        return this;
    }

    public FakeLocationReferences reference(LocationRef location, LocationReferenceView reference) {
        references.computeIfAbsent(location, ignored -> new ArrayList<>()).add(reference);
        historical.add(location);
        return this;
    }

    public void clearCurrent(LocationRef location) {
        references.remove(location);
    }

    @Override
    public List<LocationReferenceView> currentReferences(LocationRef location) {
        return List.copyOf(references.getOrDefault(location, List.of()));
    }

    @Override
    public boolean everReferenced(LocationRef location) {
        return historical.contains(location);
    }

    public void reset() {
        references.clear();
        historical.clear();
    }
}
