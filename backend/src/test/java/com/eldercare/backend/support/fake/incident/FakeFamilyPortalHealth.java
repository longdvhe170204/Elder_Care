package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.health.api.dto.VitalSignReadingView;
import com.eldercare.backend.incident.api.FamilyPortalHealthApi;
import com.eldercare.backend.incident.api.dto.FamilyIncidentView;
import java.time.Instant;
import java.util.*;

/** Dữ liệu đã được phép chia sẻ do test đặt cho từng cặp người cao tuổi/người thân. */
public class FakeFamilyPortalHealth implements FamilyPortalHealthApi {
    private record Pair(long residentId, long familyId) {}

    private final Map<Pair, List<FamilyIncidentView>> incidents = new HashMap<>();
    private final Map<Pair, List<VitalSignReadingView>> readings = new HashMap<>();

    public void incidents(long resident, long family, List<FamilyIncidentView> rows) {
        incidents.put(new Pair(resident, family), List.copyOf(rows));
    }

    public void vitalSigns(long resident, long family, List<VitalSignReadingView> rows) {
        readings.put(new Pair(resident, family), List.copyOf(rows));
    }

    @Override
    public List<FamilyIncidentView> incidents(long resident, long family, Instant from, Instant to) {
        return incidents.getOrDefault(new Pair(resident, family), List.of()).stream()
                .filter(r -> !r.general().occurredAt().isBefore(from)
                        && r.general().occurredAt().isBefore(to))
                .toList();
    }

    @Override
    public List<VitalSignReadingView> vitalSigns(long resident, long family, Instant from, Instant to) {
        return readings.getOrDefault(new Pair(resident, family), List.of()).stream()
                .filter(r -> !r.measuredAt().isBefore(from) && r.measuredAt().isBefore(to))
                .toList();
    }
}
