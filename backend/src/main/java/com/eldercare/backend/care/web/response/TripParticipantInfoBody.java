package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.api.dto.TripParticipantSources.CarriedItem;
import com.eldercare.backend.care.api.dto.TripParticipantSources.OpenEvent;
import com.eldercare.backend.care.api.dto.TripParticipantSources.TripDose;
import com.eldercare.backend.care.internal.trip.service.TripParticipantInfo.Info;
import com.eldercare.backend.care.internal.trip.service.TripParticipantInfo.Section;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * api-014 {@code TripParticipantInfo} (FR-036, FR-038, FR-039; chủ dự án duyệt 2026-11-10): sáu phần của một người tham
 * gia, mỗi phần có {@code availability} AVAILABLE, NO_DATA, SOURCE_UNAVAILABLE, NOT_PERMITTED; chỉ AVAILABLE có dữ liệu.
 */
@Schema(name = "TripParticipantInfo")
public record TripParticipantInfoBody(
        Doses medicationDoses,
        Carried carriedMedication,
        Health health,
        CareNotes careNotes,
        Contact primaryContact,
        Card emergencyCard) {

    @Schema(name = "TripDoses")
    public record Doses(
            @Schema(allowableValues = {"AVAILABLE", "NO_DATA", "SOURCE_UNAVAILABLE", "NOT_PERMITTED"})
            String availability,

            List<Dose> items) {}

    @Schema(name = "TripDose")
    public record Dose(
            String doseKey,
            Long prescriptionId,
            String medicationName,
            String strength,
            Instant scheduledTime,
            String status) {}

    @Schema(name = "TripCarriedMedication")
    public record Carried(
            @Schema(allowableValues = {"AVAILABLE", "NO_DATA", "SOURCE_UNAVAILABLE", "NOT_PERMITTED"})
            String availability,

            Instant handedAt,
            String recipientName,
            List<CarriedLine> items,
            String notCarriedReason) {}

    @Schema(name = "TripCarriedLine")
    public record CarriedLine(String medicationName, String strength, BigDecimal quantity, String unit) {}

    @Schema(name = "TripHealth")
    public record Health(
            @Schema(allowableValues = {"AVAILABLE", "NO_DATA", "SOURCE_UNAVAILABLE", "NOT_PERMITTED"})
            String availability,

            Boolean suspectedInfection,
            List<Event> openAlerts,
            List<Event> openIncidents) {}

    @Schema(name = "TripOpenEvent")
    public record Event(Long id, String type, String level, String status) {}

    @Schema(name = "TripCareNotes")
    public record CareNotes(
            @Schema(allowableValues = {"AVAILABLE", "NO_DATA", "SOURCE_UNAVAILABLE", "NOT_PERMITTED"})
            String availability,

            String text) {}

    @Schema(name = "TripPrimaryContact")
    public record Contact(
            @Schema(allowableValues = {"AVAILABLE", "NO_DATA", "SOURCE_UNAVAILABLE", "NOT_PERMITTED"})
            String availability,

            String fullName,

            @Schema(description = "Chỉ Trưởng tầng và trưởng đoàn (FR-039)")
            String phoneNumber) {}

    @Schema(name = "TripEmergencyCard")
    public record Card(
            @Schema(allowableValues = {"AVAILABLE", "NO_DATA", "SOURCE_UNAVAILABLE", "NOT_PERMITTED"})
            String availability,

            Boolean identificationOnly,
            String fullName,
            LocalDate dateOfBirth,
            String roomBed,
            String endOfLifeWish,
            Boolean criticalFlag,
            List<String> allergies,
            List<String> currentMedications,
            List<String> chronicConditions,
            List<String> unavailable,
            Instant snapshotAt) {}

    public static TripParticipantInfoBody of(Info info) {
        var doses = info.medicationDoses();
        var carried = info.carriedMedication();
        var health = info.health();
        var card = info.emergencyCard();
        return new TripParticipantInfoBody(
                new Doses(
                        name(doses),
                        doses.data() == null
                                ? null
                                : doses.data().stream()
                                        .map(TripParticipantInfoBody::dose)
                                        .toList()),
                carried.data() == null
                        ? new Carried(name(carried), null, null, null, null)
                        : new Carried(
                                name(carried),
                                carried.data().handedAt(),
                                carried.data().recipientName(),
                                carried.data().items().stream()
                                        .map(TripParticipantInfoBody::line)
                                        .toList(),
                                carried.data().notCarriedReason()),
                health.data() == null
                        ? new Health(name(health), null, null, null)
                        : new Health(
                                name(health),
                                health.data().suspectedInfection(),
                                events(health.data().openAlerts()),
                                events(health.data().openIncidents())),
                new CareNotes(name(info.careNotes()), info.careNotes().data()),
                info.primaryContact().data() == null
                        ? new Contact(name(info.primaryContact()), null, null)
                        : new Contact(
                                name(info.primaryContact()),
                                info.primaryContact().data().fullName(),
                                info.primaryContact().data().phoneNumber()),
                card.data() == null
                        ? new Card(name(card), null, null, null, null, null, null, null, null, null, null, null)
                        : card(name(card), card.data()));
    }

    private static Card card(
            String availability, com.eldercare.backend.care.api.dto.TripParticipantSources.TripEmergencyCard c) {
        return new Card(
                availability,
                c.identificationOnly(),
                c.fullName(),
                c.dateOfBirth(),
                c.roomBed(),
                c.endOfLifeWish(),
                c.criticalFlag(),
                c.allergies(),
                c.currentMedications(),
                c.chronicConditions(),
                c.unavailable(),
                c.snapshotAt());
    }

    private static String name(Section<?> section) {
        return section.availability().name();
    }

    private static Dose dose(TripDose d) {
        return new Dose(
                d.doseKey(), d.prescriptionId(), d.medicationName(), d.strength(), d.scheduledTime(), d.status());
    }

    private static CarriedLine line(CarriedItem i) {
        return new CarriedLine(i.medicationName(), i.strength(), i.quantity(), i.unit());
    }

    private static List<Event> events(List<OpenEvent> events) {
        return events.stream()
                .map(e -> new Event(e.id(), e.type(), e.level(), e.status()))
                .toList();
    }
}
