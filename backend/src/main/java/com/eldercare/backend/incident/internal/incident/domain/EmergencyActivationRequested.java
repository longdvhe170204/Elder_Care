package com.eldercare.backend.incident.internal.incident.domain;

import java.time.Instant;

/**
 * Sự kiện trong module (không ra ngoài M05): sự cố của người cao tuổi vừa ở mức Khẩn cấp, do ghi nhận ở mức Khẩn cấp
 * (kể cả chuyển, kích hoạt từ cảnh báo) hoặc nâng lên Khẩn cấp (FR-047; research S-12). Bộ nghe chạy đồng bộ trong
 * transaction của lệnh: dựng thẻ, lưu bản chụp, phát {@code EmergencyIncidentActivated}. {@code noOnDutyDoctor} rỗng
 * khi lệnh nâng mức chưa tính; khi đó bộ nghe tính, gắn dấu FR-047b và gửi thông báo khẩn cấp (lệnh ghi nhận đã gửi
 * cùng thông báo ghi nhận).
 */
public record EmergencyActivationRequested(
        long incidentId, long residentId, Long floorAreaId, Boolean noOnDutyDoctor, long actorId, Instant at) {}
