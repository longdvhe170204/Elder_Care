package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.emergency.domain.EmergencyCard;
import com.eldercare.backend.incident.web.request.RecordIncidentRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Kết quả ghi nhận hoặc kích hoạt, schema {@code ActivationResult} của api-007. {@code emergencyCard} có khi sự cố ở mức
 * Khẩn cấp (FR-047 b), lọc theo quyền xem sức khỏe của người ghi (FR-048).
 */
public record ActivationResultResponse(
        ActivationIncident incident,
        ActivationEmergencyCard emergencyCard,

        @Schema(description = "Người ghi ngoài phạm vi: chỉ nhận dạng và \"đã thông báo người xử lý\"")
        Boolean identificationOnly) {

    /** Schema {@code Incident}. */
    public record ActivationIncident(
            Long id,
            RecordIncidentRequest content,
            Long currentTypeId,

            @Schema(allowableValues = {"NEW", "IN_PROGRESS", "MONITORING", "CLOSED", "VOIDED"})
            String status,

            IncidentLevel level,
            Boolean wasEmergency,
            Integer escalationLevel,
            String assignee,
            Instant receivedAt,
            Boolean outOfScope,
            Boolean noOnDutyDoctor,
            List<Map<String, Object>> progressions,
            List<Map<String, Object>> levelChanges,
            List<String> handlers,
            List<ActivationNotifiedRecipient> notifiedRecipients,
            String resolution,
            List<String> allowedCommands,
            List<String> hiddenFields) {}

    public record ActivationNotifiedRecipient(String recipient, String channel, Instant sentAt, Instant viewedAt) {}

    /** Schema {@code EmergencyCard}. */
    public record ActivationEmergencyCard(
            EmergencyCardIdentification identification,
            Map<String, Object> endOfLifeWish,
            Boolean criticalFlag,
            List<Map<String, Object>> allergies,
            List<Map<String, Object>> currentMedications,
            List<Map<String, Object>> chronicConditions,
            String careNotes,
            EmergencyCardPrimaryContact primaryContact,
            List<String> unavailable,
            Instant snapshotAt) {

        public static ActivationEmergencyCard of(EmergencyCard card) {
            var id = card.identification();
            var contact = card.primaryContact();
            return new ActivationEmergencyCard(
                    id == null
                            ? null
                            : new EmergencyCardIdentification(
                                    id.fullName(), id.photo(), id.dateOfBirth(), id.roomBed()),
                    card.endOfLifeWish(),
                    card.criticalFlag(),
                    card.allergies(),
                    card.currentMedications(),
                    card.chronicConditions(),
                    card.careNotes(),
                    contact == null ? null : new EmergencyCardPrimaryContact(contact.fullName(), contact.phoneNumber()),
                    card.unavailable(),
                    card.snapshotAt());
        }
    }

    public record EmergencyCardIdentification(String fullName, String photo, LocalDate dateOfBirth, String roomBed) {}

    public record EmergencyCardPrimaryContact(String fullName, String phoneNumber) {}
}
