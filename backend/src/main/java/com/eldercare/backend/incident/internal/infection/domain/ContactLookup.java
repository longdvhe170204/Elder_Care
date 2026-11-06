package com.eldercare.backend.incident.internal.infection.domain;

import java.time.Instant;
import java.util.List;

/** Kết quả tra cứu truy vết (FR-060): chỉ đọc, không lưu, không đề xuất (Q-307). */
public record ContactLookup(
        Instant traceFrom, Instant traceTo, List<AbsencePeriod> suspectedPersonAbsences, List<Group> groups) {
    public ContactLookup {
        suspectedPersonAbsences = List.copyOf(suspectedPersonAbsences);
        groups = List.copyOf(groups);
    }

    /** Người tra được của một nhóm; {@code noData} khi nguồn không có ai trong khoảng. */
    public record Group(ContactGroup group, List<ContactPersonContent> people) {
        public Group {
            people = List.copyOf(people);
        }

        public boolean noData() {
            return people.isEmpty();
        }
    }
}
