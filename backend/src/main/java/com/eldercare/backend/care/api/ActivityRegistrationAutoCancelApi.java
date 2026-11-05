package com.eldercare.backend.care.api;

import java.time.Instant;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 US2 (FR-021, FR-021a; chủ dự án duyệt 2026-11-05): 007 gọi trong cùng transaction của lệnh tạo khoanh vùng, gắn
 * dấu nghi nhiễm, thay cho việc 014 nghe {@code IsolationCreated}, {@code InfectionSuspicionChanged} (care không phụ
 * thuộc incident, C-02). Gỡ khoanh vùng, gỡ dấu không khôi phục gì nên không có hàm tương ứng.
 */
public interface ActivityRegistrationAutoCancelApi {
    /** Hủy buổi chưa điểm danh có địa điểm trong vùng, đăng ký chưa diễn ra của người có giường trong vùng. */
    @Transactional(propagation = Propagation.MANDATORY)
    void cancelForIsolation(long isolationZoneId, Instant at);

    /** Hủy đăng ký chưa diễn ra của người cao tuổi ở hoạt động nhóm (chuyến đi thêm ở US4). */
    @Transactional(propagation = Propagation.MANDATORY)
    void cancelForSuspectedInfection(long residentId, Instant at);
}
