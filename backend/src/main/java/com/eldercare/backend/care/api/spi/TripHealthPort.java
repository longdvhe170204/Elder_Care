package com.eldercare.backend.care.api.spi;

import com.eldercare.backend.care.api.dto.TripParticipantSources.TripEmergencyCard;
import com.eldercare.backend.care.api.dto.TripParticipantSources.TripHealth;
import java.time.Instant;

/**
 * 007 cài nguồn sức khỏe cho TripDetail (014 FR-039; chủ dự án duyệt 2026-11-10): care khai, incident cài, không tạo phụ
 * thuộc care → incident. Lọc quyền nhóm sức khỏe do care quyết định trước khi gọi ({@code fullHealth}).
 */
public interface TripHealthPort {

    /** Dấu nghi nhiễm, cảnh báo và sự cố đang mở của người cao tuổi tại {@code at}. */
    TripHealth health(long residentId, Instant at);

    /** Thẻ khẩn cấp dựng tại {@code at}; {@code fullHealth} sai thì chỉ có nhận dạng (007 FR-048). */
    TripEmergencyCard emergencyCard(long residentId, Instant at, boolean fullHealth);
}
