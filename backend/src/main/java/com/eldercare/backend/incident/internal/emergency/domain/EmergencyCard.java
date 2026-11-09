package com.eldercare.backend.incident.internal.emergency.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Thẻ thông tin khẩn cấp (FR-048; schema {@code EmergencyCard} của api-007), cũng là nội dung bản chụp lưu cùng sự cố
 * mỗi lần kích hoạt (data-model {@code ban_chup_the_khan_cap}, R-15 → {@code record_event} {@code M05.EMERGENCY_CARD}).
 * {@code unavailable} liệt kê mục không lấy được vì nguồn lỗi; thẻ vẫn lưu.
 */
public record EmergencyCard(
        Identification identification,
        Map<String, Object> endOfLifeWish,
        Boolean criticalFlag,
        List<Map<String, Object>> allergies,
        List<Map<String, Object>> currentMedications,
        List<Map<String, Object>> chronicConditions,
        String careNotes,
        Contact primaryContact,
        List<String> unavailable,
        Instant snapshotAt) {

    public record Identification(String fullName, String photo, LocalDate dateOfBirth, String roomBed) {}

    public record Contact(String fullName, String phoneNumber) {}

    /** Người không được xem nhóm sức khỏe chỉ thấy nhận dạng (FR-048). */
    public EmergencyCard identificationOnly() {
        return new EmergencyCard(identification, null, null, null, null, null, null, null, null, snapshotAt);
    }
}
