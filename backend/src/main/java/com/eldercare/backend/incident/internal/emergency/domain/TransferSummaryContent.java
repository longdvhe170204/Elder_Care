package com.eldercare.backend.incident.internal.emergency.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Bản tóm tắt chuyển viện (FR-050; data-model {@code ban_tom_tat_chuyen_vien}, R-15 → {@code record_event}
 * {@code M05.TRANSFER_SUMMARY} cha {@code M05.INCIDENT}): bản chụp lúc lệnh, phiếu PDF dựng từ đúng nội dung này.
 */
public record TransferSummaryContent(
        long residentId,
        String residentName,
        String receivingFacility,
        Instant transferAt,
        Instant expectedReturnAt,
        String companion,
        Boolean requiredContactReached,
        String notAskedFirstReason,
        List<Map<String, Object>> allergies,
        List<Map<String, Object>> currentMedications,
        List<Map<String, Object>> latestVitalSigns,
        List<Map<String, Object>> progressions,
        Map<String, Object> endOfLifeWish,
        Map<String, Object> treatmentWish,
        Long treatmentWishVersionId,
        EmergencyCard.Contact primaryContact,
        long documentFileId,
        long preparedById,
        String preparedBy,
        Instant preparedAt,
        long statusHistoryId) {}
