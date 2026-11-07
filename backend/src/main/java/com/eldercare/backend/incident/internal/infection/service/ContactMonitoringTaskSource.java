package com.eldercare.backend.incident.internal.infection.service;

import com.eldercare.backend.care.api.spi.TaskSourcePort;
import com.eldercare.backend.incident.internal.infection.persistence.ContactPersonStore;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Nguồn công việc {@code CONTACT_MONITORING} cho 005 (research S-15): căn cứ là mã người tiếp xúc; hợp lệ khi dòng chưa
 * bị loại, đúng người cao tuổi và thời điểm dự kiến trong khoảng theo dõi. Người phụ trách ca tự lập công việc (Q-307).
 */
@Component
public class ContactMonitoringTaskSource implements TaskSourcePort {
    private final ContactPersonStore people;

    public ContactMonitoringTaskSource(ContactPersonStore people) {
        this.people = people;
    }

    @Override
    public Set<SourceType> managedTypes() {
        return Set.of(SourceType.CONTACT_MONITORING);
    }

    @Override
    public SourceValidation validate(SourceType type, long sourceId, long residentId, Instant plannedAt) {
        var row = people.monitored(residentId).stream()
                .filter(r -> r.id() == sourceId && ContactPersonStore.ACTIVE.equals(r.status()))
                .findFirst();
        if (row.isEmpty()) return new SourceValidation(false, false, false, false);
        boolean inside = plannedAt != null && row.get().content().monitoredAt(plannedAt);
        return new SourceValidation(inside, true, inside, false);
    }

    @Override
    public List<SourceView> sources(long residentId, Instant from, Instant to) {
        return people.monitored(residentId).stream()
                .filter(r -> ContactPersonStore.ACTIVE.equals(r.status()))
                .filter(r -> r.content().monitoredFrom().isBefore(to)
                        && r.content().monitoredTo().isAfter(from))
                .map(r -> new SourceView(
                        SourceType.CONTACT_MONITORING,
                        r.id(),
                        r.content().monitoredFrom(),
                        "Theo dõi tiếp xúc đến " + r.content().monitoredTo()))
                .toList();
    }
}
