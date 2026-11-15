package com.eldercare.backend.care.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO của các nguồn bổ sung TripDetail (014 FR-036, FR-038, FR-039; đề xuất {@code trip-detail-proposal-2026-11-08.md},
 * chủ dự án duyệt cả 6 phần 2026-11-10): 006 trả liều trong khoảng đi và thuốc đã giao mang theo; 007 trả sức khỏe đang
 * mở và thẻ khẩn cấp. Không suy thuốc đã giao từ "đã chuẩn bị", không suy an toàn từ kết quả đánh giá Đạt.
 */
public final class TripParticipantSources {
    private TripParticipantSources() {}

    /** Một liều có thời điểm dự kiến trong khoảng rời → về của chuyến (006). */
    public record TripDose(
            String doseKey,
            long prescriptionId,
            String medicationName,
            String strength,
            Instant scheduledTime,
            String status) {}

    /** Một dòng thuốc đã giao mang theo (006 lần giao gắn chuyến). */
    public record CarriedItem(String medicationName, String strength, BigDecimal quantity, String unit) {}

    /** Thuốc mang theo: lần giao (thời điểm, người nhận, các dòng) hoặc quyết định không mang thuốc kèm lý do. */
    public record CarriedMedication(
            Instant handedAt, String recipientName, List<CarriedItem> items, String notCarriedReason) {}

    /** Cảnh báo, sự cố đang mở của người cao tuổi (007). */
    public record OpenEvent(long id, String type, String level, String status) {}

    /** Sức khỏe đang mở (007): dấu nghi nhiễm, cảnh báo, sự cố chưa đóng. */
    public record TripHealth(boolean suspectedInfection, List<OpenEvent> openAlerts, List<OpenEvent> openIncidents) {}

    /**
     * Thẻ khẩn cấp theo người (007 FR-048): người xem không được xem đủ nhóm sức khỏe thì chỉ có nhận dạng
     * ({@code identificationOnly}); {@code unavailable} là mục nguồn lỗi.
     */
    public record TripEmergencyCard(
            boolean identificationOnly,
            String fullName,
            LocalDate dateOfBirth,
            String roomBed,
            String endOfLifeWish,
            Boolean criticalFlag,
            List<String> allergies,
            List<String> currentMedications,
            List<String> chronicConditions,
            String careNotes,
            String primaryContactName,
            String primaryContactPhone,
            List<String> unavailable,
            Instant snapshotAt) {}
}
