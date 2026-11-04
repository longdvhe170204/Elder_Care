package com.eldercare.backend.family.api.dto;

import java.time.Instant;

/**
 * Buổi hoạt động sắp tới trên kênh người cao tuổi ({@code BuoiSapToiView}; FR-083 (c)): buổi của tầng người cao tuổi
 * hoặc toàn viện; {@code registered} khi người cao tuổi đã có tên. Không có đánh giá khả năng tham gia.
 */
public record ResidentChannelSessionView(
        long sessionId, String name, Instant startAt, Instant endAt, boolean facilityWide, boolean registered) {}
