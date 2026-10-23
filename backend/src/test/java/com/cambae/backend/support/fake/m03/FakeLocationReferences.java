package com.cambae.backend.support.fake.m03;

import com.cambae.backend.m03.api.dto.*;
import com.cambae.backend.m03.api.spi.LocationReferences;
import java.util.*;

/** Bản giả duy nhất của SPI 003, chỉ test import cấu hình mới nạp. */
public class FakeLocationReferences implements LocationReferences {
    private final Map<LocationRef, List<LocationReferenceView>> current = new HashMap<>();
    private final Set<LocationRef> past = new HashSet<>();

    public FakeLocationReferences references(LocationRef location, List<LocationReferenceView> refs) {
        current.put(location, List.copyOf(refs));
        if (!refs.isEmpty()) past.add(location);
        return this;
    }

    public List<LocationReferenceView> activeReferences(LocationRef location) {
        return current.getOrDefault(location, List.of());
    }

    public boolean everReferenced(LocationRef location) {
        return past.contains(location);
    }
}
