package com.eldercare.backend.incident.api.dto;

import java.util.List;

/**
 * Chuyến người cao tuổi đang ở Hoạt động bên ngoài ({@code ChuyenDangDienRaView}, module-interfaces 2.1): trưởng đoàn
 * nhận qua nhóm {@code TripLeader(tripId)}, người đi cùng là nhân viên (FR-047e).
 */
public record SosTripView(long tripId, List<Long> companionStaffIds) {
    public SosTripView {
        companionStaffIds = List.copyOf(companionStaffIds);
    }
}
