package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.incident.api.dto.SosTripView;
import com.eldercare.backend.incident.api.spi.SosTripPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Chuyến đang diễn ra do test đặt theo người cao tuổi (T073). */
public class FakeSosTrip implements SosTripPort {
    private final Map<Long, SosTripView> trips = new HashMap<>();

    public FakeSosTrip ongoing(long residentId, SosTripView trip) {
        trips.put(residentId, trip);
        return this;
    }

    public void clear() {
        trips.clear();
    }

    @Override
    public Optional<SosTripView> ongoing(long residentId) {
        return Optional.ofNullable(trips.get(residentId));
    }
}
