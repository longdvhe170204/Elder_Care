package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.incident.api.dto.EmergencyFamilyContactView;
import com.eldercare.backend.incident.api.dto.VisitTraceView;
import com.eldercare.backend.incident.api.spi.EmergencyFamilyPort;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Nguồn người thân và lượt thăm do test đặt; không sinh liên hệ hoặc lượt thăm. */
public class FakeEmergencyFamily implements EmergencyFamilyPort {
    private final Map<Long, EmergencyFamilyContactView> primaryContacts = new HashMap<>();
    private final Map<Long, List<EmergencyFamilyContactView>> representatives = new HashMap<>();
    private final Map<Long, List<VisitTraceView>> visits = new HashMap<>();

    public FakeEmergencyFamily primaryContact(long residentId, EmergencyFamilyContactView contact) {
        primaryContacts.put(residentId, contact);
        return this;
    }

    public FakeEmergencyFamily representatives(long residentId, List<EmergencyFamilyContactView> contacts) {
        representatives.put(residentId, List.copyOf(contacts));
        return this;
    }

    public FakeEmergencyFamily visits(long residentId, List<VisitTraceView> rows) {
        visits.put(residentId, List.copyOf(rows));
        return this;
    }

    @Override
    public Optional<EmergencyFamilyContactView> primaryContact(long residentId) {
        return Optional.ofNullable(primaryContacts.get(residentId));
    }

    @Override
    public List<EmergencyFamilyContactView> representatives(long residentId) {
        return representatives.getOrDefault(residentId, List.of());
    }

    @Override
    public List<VisitTraceView> visitsInRange(long residentId, Instant from, Instant to) {
        return visits.getOrDefault(residentId, List.of()).stream()
                .filter(visit -> visit.enteredAt().isBefore(to)
                        && visit.exitedAt().map(end -> end.isAfter(from)).orElse(true))
                .toList();
    }
}
