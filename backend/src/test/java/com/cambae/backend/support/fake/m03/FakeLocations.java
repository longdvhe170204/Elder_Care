package com.cambae.backend.support.fake.m03;

import com.cambae.backend.m03.api.*;
import com.cambae.backend.m03.api.dto.*;
import java.time.*;
import java.util.*;

/** Một bản giả chung cho ba nguồn vị trí 003; khoảng nửa mở và lịch sử chuyển phòng. */
public class FakeLocations implements LocationApi, LocationStructureApi, LocationHistoryApi {
    private final Map<Long, FloorView> floors = new HashMap<>();
    private final Map<Long, RoomView> rooms = new HashMap<>();
    private final Map<Long, AreaView> areas = new HashMap<>();
    private final List<LocationStay> stays = new ArrayList<>();
    private final Set<LocationRef> disabled = new HashSet<>();
    private LocationRef daycareRestLocation;

    public FakeLocations daycareRestLocation(LocationRef ref) {
        daycareRestLocation = ref;
        return this;
    }

    public Optional<LocationRef> daycareRestLocation() {
        return Optional.ofNullable(daycareRestLocation);
    }

    public FakeLocations floor(long id, Long area) {
        floors.put(id, new FloorView(id, area, "[Tầng kiểm thử]"));
        if (area != null) areas.put(area, new AreaView(area, "[Khu kiểm thử]"));
        return this;
    }

    public FakeLocations room(long id, long floor) {
        rooms.put(id, new RoomView(id, floor, "[Phòng kiểm thử]"));
        return this;
    }

    public FakeLocations disable(LocationRef ref) {
        disabled.add(ref);
        return this;
    }

    public FakeLocations stay(long resident, long room, Instant from, Instant to) {
        var r = rooms.get(room);
        var f = floors.get(r.floorAreaId());
        stays.add(new LocationStay(
                resident,
                "[Họ tên NCT kiểm thử]",
                new LocationView(f.areaId(), f.id(), room, null, "[Vị trí kiểm thử]"),
                from,
                to,
                false,
                false,
                List.of()));
        return this;
    }

    public FakeLocations closeStays(long resident, Instant at) {
        for (int i = 0; i < stays.size(); i++) {
            var s = stays.get(i);
            if (s.residentId() == resident && !s.from().isAfter(at) && (s.to() == null || s.to().isAfter(at)))
                stays.set(
                        i,
                        new LocationStay(
                                s.residentId(),
                                s.fullName(),
                                s.location(),
                                s.from(),
                                at,
                                s.corrected(),
                                s.pendingLateEntry(),
                                s.absences()));
        }
        return this;
    }

    public Optional<LocationView> at(long id, Instant at) {
        return stays.stream()
                .filter(s -> s.residentId() == id && covers(s, at))
                .map(LocationStay::location)
                .findFirst();
    }

    public Optional<RoomView> room(long id) {
        return Optional.ofNullable(rooms.get(id));
    }

    public Optional<FloorView> floor(long id) {
        return Optional.ofNullable(floors.get(id));
    }

    public Optional<AreaView> area(long id) {
        return Optional.ofNullable(areas.get(id));
    }

    public Set<Long> floorsInArea(long area) {
        var result = new HashSet<Long>();
        floors.values().stream().filter(f -> Objects.equals(f.areaId(), area)).forEach(f -> result.add(f.id()));
        return Set.copyOf(result);
    }

    public boolean active(LocationRef ref) {
        return !disabled.contains(ref)
                && switch (ref.type()) {
                    case "ROOM" -> rooms.containsKey(ref.id());
                    case "FLOOR" -> floors.containsKey(ref.id());
                    case "AREA" -> areas.containsKey(ref.id());
                    default -> false;
                };
    }

    private boolean matches(LocationRef ref, LocationStay s) {
        if (ref == null) return true;
        return switch (ref.type()) {
            case "ROOM" -> s.location().roomId() == ref.id();
            case "FLOOR" -> Objects.equals(s.location().floorAreaId(), ref.id());
            case "AREA" -> Objects.equals(s.location().areaId(), ref.id());
            default -> false;
        };
    }

    private boolean covers(LocationStay s, Instant at) {
        return !s.from().isAfter(at) && (s.to() == null || s.to().isAfter(at));
    }

    public List<LocationStay> atLocation(LocationRef ref, Instant at) {
        return stays.stream().filter(s -> matches(ref, s) && covers(s, at)).toList();
    }

    public List<LocationStay> inDay(LocationRef ref, LocalDate day) {
        var from = day.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        return stays.stream()
                .filter(s -> matches(ref, s)
                        && s.from().isBefore(from.plusSeconds(86400))
                        && (s.to() == null || s.to().isAfter(from)))
                .toList();
    }

    public List<LocationStay> byResident(long resident, Instant from, Instant to) {
        return stays.stream()
                .filter(s ->
                        s.residentId() == resident && s.from().isBefore(to) && (s.to() == null || s.to().isAfter(from)))
                .toList();
    }
}
