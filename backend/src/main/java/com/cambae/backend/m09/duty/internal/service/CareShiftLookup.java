package com.cambae.backend.m09.duty.internal.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.m04.api.dto.CareShiftView;
import com.cambae.backend.m04.api.spi.CareShiftPort;
import com.cambae.backend.m09.shift.internal.domain.WorkShift;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** Bản cài phần ca của NhanSuCaPort; không giả dữ liệu phân công hoặc bàn giao chưa có. */
@Service
class CareShiftLookup implements CareShiftPort {
    private final ShiftQueries query;

    CareShiftLookup(ShiftQueries query) {
        this.query = query;
    }

    private CareShiftView view(WorkShift s) {
        return new CareShiftView(s.id(), s.scopeId(), s.startsAt(), s.endsAt(), query.published(s));
    }

    public Optional<CareShiftView> shift(long id) {
        return query.find(id).map(this::view);
    }

    public List<CareShiftView> publishedShifts(long floor, Instant from, Instant to) {
        return query.published(floor, from, to).stream().map(this::view).toList();
    }

    public Optional<CareShiftView> shiftAt(long floor, Instant at) {
        return query.atFloor(floor, at).map(this::view);
    }

    public Optional<Long> shiftLead(long id) {
        return query.find(id).filter(s -> !s.missingLead()).map(WorkShift::leadStaffId);
    }

    public boolean listedInShift(long staff, Instant at) {
        return query.inProgressMember(staff, at);
    }

    public boolean qualified(long staff, Set<String> certificates, LocalDate date) {
        return query.qualified(staff, certificates, date);
    }

    public boolean hasQualifiedStaff(long id, Role role, Set<String> certificates) {
        return query.anyQualified(id, role, certificates);
    }
}
