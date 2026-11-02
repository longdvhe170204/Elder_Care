package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.incident.api.IsolationApi;
import com.eldercare.backend.incident.api.dto.IsolationZoneView;
import java.time.Instant;
import java.util.*;

/** Các vùng có hiệu lực do test đặt; không tạo hoặc gỡ vùng thay actor. */
public class FakeIsolation implements IsolationApi {
    private List<IsolationZoneView> zones = List.of();
    private final Map<Long, List<IsolationZoneView>> rooms = new HashMap<>();
    private final Map<Long, List<IsolationZoneView>> residents = new HashMap<>();

    public void zones(List<IsolationZoneView> rows) {
        zones = List.copyOf(rows);
    }

    public void room(long id, List<IsolationZoneView> rows) {
        rooms.put(id, List.copyOf(rows));
    }

    public void resident(long id, List<IsolationZoneView> rows) {
        residents.put(id, List.copyOf(rows));
    }

    private List<IsolationZoneView> at(List<IsolationZoneView> rows, Instant at) {
        return rows.stream()
                .filter(r -> !r.startsAt().isAfter(at) && (r.endsAt() == null || at.isBefore(r.endsAt())))
                .toList();
    }

    @Override
    public List<IsolationZoneView> activeByRoom(long id, Instant at) {
        return at(rooms.getOrDefault(id, List.of()), at);
    }

    @Override
    public List<IsolationZoneView> activeByFloorArea(long id, Instant at) {
        return at(zones.stream().filter(r -> r.floorAreaIds().contains(id)).toList(), at);
    }

    @Override
    public List<IsolationZoneView> activeByResident(long id, Instant at) {
        return at(residents.getOrDefault(id, List.of()), at);
    }

    @Override
    public List<IsolationZoneView> activeZones() {
        return zones;
    }
}
