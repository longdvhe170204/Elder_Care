package com.eldercare.backend.incident.internal.emergency.service;

import com.eldercare.backend.care.api.dto.TripParticipantSources.OpenEvent;
import com.eldercare.backend.care.api.dto.TripParticipantSources.TripEmergencyCard;
import com.eldercare.backend.care.api.dto.TripParticipantSources.TripHealth;
import com.eldercare.backend.care.api.spi.TripHealthPort;
import com.eldercare.backend.incident.api.AlertApi;
import com.eldercare.backend.incident.api.IncidentApi;
import com.eldercare.backend.incident.internal.emergency.domain.EmergencyCard;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@code care.api.spi.TripHealthPort} của 014 (TripDetail FR-039; chủ dự án duyệt 2026-11-10): dấu nghi nhiễm, cảnh
 * báo và sự cố đang mở, thẻ khẩn cấp dựng theo người bằng {@link EmergencyCardService#build}. Care quyết định người xem có
 * được xem đủ nhóm sức khỏe; không đủ thì chỉ trả nhận dạng như 007 FR-048.
 */
@Component
public class CareTripHealth implements TripHealthPort {

    private static final Set<String> OPEN_ALERTS = Set.of("NEW", "ESCALATED", "RECEIVED", "IN_PROGRESS");
    private static final Set<String> OPEN_INCIDENTS = Set.of("NEW", "IN_PROGRESS", "MONITORING");

    private final IncidentApi incidents;
    private final AlertApi alerts;
    private final EmergencyCardService cards;

    CareTripHealth(IncidentApi incidents, AlertApi alerts, EmergencyCardService cards) {
        this.incidents = incidents;
        this.alerts = alerts;
        this.cards = cards;
    }

    @Override
    @Transactional(readOnly = true)
    public TripHealth health(long residentId, Instant at) {
        return new TripHealth(
                incidents.monitoringFlag(residentId, at).suspectedInfection(),
                alerts.byResident(residentId, Instant.EPOCH, at).stream()
                        .filter(a -> OPEN_ALERTS.contains(a.status()))
                        .map(a -> new OpenEvent(a.id(), a.type(), a.level(), a.status()))
                        .toList(),
                incidents.byResident(residentId, Instant.EPOCH, at, true).stream()
                        .filter(i -> OPEN_INCIDENTS.contains(i.status()))
                        .map(i -> new OpenEvent(i.id(), i.type(), i.level(), i.status()))
                        .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TripEmergencyCard emergencyCard(long residentId, Instant at, boolean fullHealth) {
        EmergencyCard card = cards.build(residentId, at);
        if (!fullHealth) card = card.identificationOnly();
        var id = card.identification();
        return new TripEmergencyCard(
                !fullHealth,
                id == null ? null : id.fullName(),
                id == null ? null : id.dateOfBirth(),
                id == null ? null : id.roomBed(),
                wish(card.endOfLifeWish()),
                card.criticalFlag(),
                names(card.allergies()),
                names(card.currentMedications()),
                names(card.chronicConditions()),
                card.careNotes(),
                card.primaryContact() == null ? null : card.primaryContact().fullName(),
                card.primaryContact() == null ? null : card.primaryContact().phoneNumber(),
                card.unavailable() == null ? List.of() : card.unavailable(),
                card.snapshotAt());
    }

    /** Lựa chọn nguyện vọng cuối đời hiện hành; chưa ghi thì {@code NOT_RECORDED}. */
    private static String wish(Map<String, Object> wish) {
        if (wish == null) return null;
        if (Boolean.TRUE.equals(wish.get("notRecorded"))) return "NOT_RECORDED";
        return Objects.toString(wish.get("choice"), null);
    }

    private static List<String> names(List<Map<String, Object>> items) {
        if (items == null) return null;
        return items.stream()
                .map(item -> Objects.toString(item.get("name"), null))
                .filter(Objects::nonNull)
                .toList();
    }
}
