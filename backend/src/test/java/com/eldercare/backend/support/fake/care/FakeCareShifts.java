package com.eldercare.backend.support.fake.care;

import com.eldercare.backend.care.api.dto.CareShiftView;
import com.eldercare.backend.care.api.spi.CareShiftPort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;

/** Dữ liệu do test đặt; ca, thành viên và hiệu lực chứng chỉ là các nguồn độc lập. */
public class FakeCareShifts implements CareShiftPort {
    private final Set<Long> confirmedHandovers = new HashSet<>();

    public FakeCareShifts handoverConfirmed(long id, boolean value) {
        if (value) confirmedHandovers.add(id);
        else confirmedHandovers.remove(id);
        return this;
    }

    public boolean handoverConfirmed(long id) {
        return confirmedHandovers.contains(id);
    }

    private record Entry(CareShiftView view, List<Long> members, Long lead) {}

    private record Qualification(Role role, Set<String> certificates, LocalDate from, LocalDate to) {}

    private final Map<Long, Entry> shifts = new LinkedHashMap<>();
    private final Map<Long, Qualification> qualifications = new HashMap<>();

    private record AssignmentKey(long resident, long shift) {}

    private final Map<AssignmentKey, List<com.eldercare.backend.care.api.dto.CareAssignmentView>> assignments =
            new HashMap<>();

    public FakeCareShifts assignments(
            long resident, long shift, List<com.eldercare.backend.care.api.dto.CareAssignmentView> rows) {
        assignments.put(new AssignmentKey(resident, shift), List.copyOf(rows));
        return this;
    }

    public List<com.eldercare.backend.care.api.dto.CareAssignmentView> assignments(long resident, long shift) {
        return assignments.getOrDefault(new AssignmentKey(resident, shift), List.of());
    }

    public List<Long> assignedNurses(long resident, Instant at) {
        return shifts.values().stream()
                .filter(e ->
                        !e.view().startsAt().isAfter(at) && e.view().endsAt().isAfter(at))
                .flatMap(e -> assignments(resident, e.view().id()).stream())
                .filter(a -> a.assignedNurse())
                .map(a -> a.staffId())
                .distinct()
                .toList();
    }

    public FakeCareShifts shift(CareShiftView view, List<Long> members, Long lead) {
        shifts.put(view.id(), new Entry(view, List.copyOf(members), lead));
        return this;
    }

    public FakeCareShifts qualification(long staff, Role role, Set<String> certificates, LocalDate from, LocalDate to) {
        qualifications.put(staff, new Qualification(role, Set.copyOf(certificates), from, to));
        return this;
    }

    public Optional<CareShiftView> shift(long id) {
        return Optional.ofNullable(shifts.get(id)).map(Entry::view);
    }

    public List<CareShiftView> publishedShifts(long floor, Instant from, Instant to) {
        return shifts.values().stream()
                .map(Entry::view)
                .filter(s -> s.published()
                        && Objects.equals(s.floorAreaId(), floor)
                        && s.startsAt().isBefore(to)
                        && s.endsAt().isAfter(from))
                .sorted(Comparator.comparing(CareShiftView::startsAt))
                .toList();
    }

    public Optional<CareShiftView> shiftAt(long floor, Instant at) {
        return publishedShifts(floor, at, at.plusNanos(1)).stream().findFirst();
    }

    public Optional<Long> shiftLead(long id) {
        return Optional.ofNullable(shifts.get(id)).map(Entry::lead);
    }

    public boolean listedInShift(long staff, Instant at) {
        return shifts.values().stream()
                .anyMatch(s -> s.view().published()
                        && s.members().contains(staff)
                        && !s.view().startsAt().isAfter(at)
                        && s.view().endsAt().isAfter(at));
    }

    public boolean qualified(long staff, Set<String> certificates, LocalDate date) {
        var q = qualifications.get(staff);
        return q != null
                && q.certificates().containsAll(certificates)
                && !date.isBefore(q.from())
                && (q.to() == null || !date.isAfter(q.to()));
    }

    public boolean hasQualifiedStaff(long id, Role role, Set<String> certificates) {
        var s = shifts.get(id);
        return s != null
                && s.members().stream()
                        .anyMatch(staff -> qualifications.containsKey(staff)
                                && qualifications.get(staff).role() == role
                                && qualified(
                                        staff,
                                        certificates,
                                        LocalDate.ofInstant(s.view().startsAt(), BusinessTime.ZONE)));
    }
}
