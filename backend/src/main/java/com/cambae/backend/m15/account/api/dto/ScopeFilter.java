package com.cambae.backend.m15.account.api.dto;

import java.util.Set;

/**
 * Bộ lọc phạm vi cho danh sách, tìm kiếm, số đếm (FR-035; 002 research A-07): toàn viện, hoặc tập tầng/khu vực, phòng,
 * người cao tuổi để module chuyển thành điều kiện {@code IN (...)}.
 */
public record ScopeFilter(boolean facilityWide, Set<Long> floorAreaIds, Set<Long> roomIds, Set<Long> residentIds) {

    public static final ScopeFilter FACILITY = new ScopeFilter(true, Set.of(), Set.of(), Set.of());
}
