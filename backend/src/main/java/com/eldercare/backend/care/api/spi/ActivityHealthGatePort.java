package com.eldercare.backend.care.api.spi;

import java.time.Instant;

/**
 * 014 US2 (FR-015 (c), (g), FR-018, FR-021; chủ dự án duyệt 2026-11-05): nguồn 007 cho điều kiện đăng ký hoạt động. Khai ở
 * care theo mẫu C-02 của 005 (care không phụ thuộc incident); 007 cài một bean chỉ đọc. Chưa có bean thì 014 chặn đăng
 * ký bằng 503 {@code CARE_SOURCE_UNAVAILABLE} (mặc định an toàn).
 */
public interface ActivityHealthGatePort {
    /** Giường hoặc khu nghỉ bán trú của người cao tuổi thuộc vùng Đang khoanh vùng (007 FR-065). */
    boolean residentIsolated(long residentId, Instant at);

    /** Địa điểm (phòng, khu vực, tầng/khu) thuộc vùng Đang khoanh vùng; tham số không có thì {@code null}. */
    boolean locationIsolated(Long roomId, Long areaId, Long floorAreaId, Instant at);

    /** Người cao tuổi đang mang dấu "nghi nhiễm" (007 FR-060). */
    boolean suspectedInfection(long residentId, Instant at);

    /** Có dấu nghi nhiễm bất kỳ lúc nào trong khoảng [from, to); ngày nghiệp vụ dùng hai mốc 00:00 (FR-054). */
    boolean suspectedInfectionDuring(long residentId, Instant from, Instant to);

    /** Có cảnh báo hoặc sự cố mức Trung bình trở lên đang mở (FR-018). */
    boolean hasOpenMediumOrHigher(long residentId);
}
