package com.eldercare.backend.support.fake.roombedallocation;

import com.eldercare.backend.roombedallocation.api.*;
import com.eldercare.backend.roombedallocation.api.dto.*;
import java.time.*;
import java.util.*;

/** Một bản giả chung cho ba nguồn vị trí 003; khoảng nửa mở và lịch sử chuyển phòng. */
public class FakeLocations implements LocationApi, ResidentLocationApi, LocationStructureApi, LocationHistoryApi {
    private final Map<Long, FloorView> floors = new HashMap<>();
    private final Map<Long, RoomView> rooms = new HashMap<>();
    private final Map<Long, AreaView> areas = new HashMap<>();
    private final List<ResidentLocationRow> stays = new ArrayList<>();
    private final Set<LocationRef> disabled = new HashSet<>();
    private LocationRef daycareRestLocation;

    public FakeLocations daycareRestLocation(LocationRef ref) {
        daycareRestLocation = ref;
        return this;
    }

    public Optional<LocationRef> dayCareRestAreaLocation() {
        return Optional.ofNullable(daycareRestLocation);
    }

    public FakeLocations floor(long id, Long area) {
        long parent = area == null ? id : area;
        floors.put(id, new FloorView(id, parent, Long.toString(id), "[Tầng kiểm thử]", null, true));
        areas.putIfAbsent(parent, new AreaView(parent, Long.toString(parent), "[Khu kiểm thử]", true));
        return this;
    }

    public FakeLocations room(long id, long floor) {
        rooms.put(id, new RoomView(id, floor, 1, Long.toString(id), "[Phòng kiểm thử]", true));
        return this;
    }

    public FakeLocations disable(LocationRef ref) {
        disabled.add(ref);
        return this;
    }

    public FakeLocations stay(long resident, long room, Instant from, Instant to) {
        var r = rooms.get(room);
        var f = floors.get(r.floorId());
        stays.add(new ResidentLocationRow(
                resident,
                "[Họ tên NCT kiểm thử]",
                new LocationView(f.areaId(), null, f.id(), f.name(), room, r.name(), resident, "[Vị trí kiểm thử]"),
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
                        new ResidentLocationRow(
                                s.residentId(),
                                s.fullName(),
                                s.location(),
                                s.from(),
                                at,
                                s.corrected(),
                                s.pendingBackfill(),
                                s.absences()));
        }
        return this;
    }

    public Optional<LocationView> at(long id, Instant at) {
        return stays.stream()
                .filter(s -> s.residentId() == id && covers(s, at))
                .map(ResidentLocationRow::location)
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

    private boolean matches(LocationRef ref, ResidentLocationRow s) {
        if (ref == null) return true;
        return switch (ref.type()) {
            case "ROOM" -> s.location().roomId() == ref.id();
            case "FLOOR" -> Objects.equals(s.location().floorId(), ref.id());
            case "AREA" -> Objects.equals(s.location().areaId(), ref.id());
            default -> false;
        };
    }

    private boolean covers(ResidentLocationRow s, Instant at) {
        return !s.from().isAfter(at) && (s.to() == null || s.to().isAfter(at));
    }

    public List<BedLocationRow> byLocation(LocationRef ref, Instant at) {
        return stays.stream()
                .filter(s -> matches(ref, s) && covers(s, at))
                .map(FakeLocations::bedRow)
                .toList();
    }

    public List<BedLocationRow> byLocationOn(LocationRef ref, LocalDate day) {
        var from = day.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        return stays.stream()
                .filter(s -> matches(ref, s)
                        && s.from().isBefore(from.plusSeconds(86400))
                        && (s.to() == null || s.to().isAfter(from)))
                .map(FakeLocations::bedRow)
                .toList();
    }

    public List<ResidentLocationRow> byResident(long resident, Instant from, Instant to) {
        return stays.stream()
                .filter(s ->
                        s.residentId() == resident && s.from().isBefore(to) && (s.to() == null || s.to().isAfter(from)))
                .toList();
    }

    private static BedLocationRow bedRow(ResidentLocationRow row) {
        return new BedLocationRow(
                row.residentId(),
                row.fullName(),
                row.location(),
                row.from(),
                row.to(),
                row.corrected(),
                row.pendingBackfill(),
                row.absences());
    }

    public Optional<LocationView> bed(long id) {
        return stays.stream()
                .map(ResidentLocationRow::location)
                .filter(row -> row.bedId() == id)
                .findFirst();
    }

    public Optional<RoomTypeRef> roomTypeOfBed(long id) {
        return Optional.empty();
    }

    public Optional<LocationView> current(long resident) {
        return stays.stream()
                .filter(row -> row.residentId() == resident && row.to() == null)
                .max(Comparator.comparing(ResidentLocationRow::from))
                .map(ResidentLocationRow::location);
    }

    public Map<Long, LocationView> current(Collection<Long> residents) {
        var found = new HashMap<Long, LocationView>();
        for (long id : residents) current(id).ifPresent(location -> found.put(id, location));
        return found;
    }

    public Optional<Long> floorOn(long resident, LocalDate date) {
        return at(resident, date.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant())
                .map(LocationView::floorId);
    }

    public boolean hasNoBed(long resident) {
        return current(resident).isEmpty();
    }

    public Optional<com.eldercare.backend.administration.account.api.spi.BedAllocationView> currentOrFutureAllocation(
            long resident) {
        return Optional.empty();
    }

    public List<RoommateView> roommates(long resident, Instant from, Instant to) {
        return List.of();
    }

    /** Cấu hình vị trí giả cho test consumer; không nhập vào các IT kiểm bản cài cơ cấu thật của C. */
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        FakeLocations fakeLocations() {
            return new FakeLocations();
        }
    }
}
