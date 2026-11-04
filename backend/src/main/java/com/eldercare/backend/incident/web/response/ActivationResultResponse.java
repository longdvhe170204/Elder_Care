package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.web.request.RecordIncidentRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Kết quả ghi nhận hoặc kích hoạt, schema {@code ActivationResult} của api-007. Thẻ khẩn cấp (mục G) chưa dựng ở lát
 * này nên {@code emergencyCard} trống.
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
            Instant snapshotAt) {}

    public record EmergencyCardIdentification(String fullName, String photo, LocalDate dateOfBirth, String roomBed) {}

    public record EmergencyCardPrimaryContact(String fullName, String phoneNumber) {}
}
