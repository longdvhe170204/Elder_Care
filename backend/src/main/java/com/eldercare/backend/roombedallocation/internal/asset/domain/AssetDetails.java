package com.eldercare.backend.roombedallocation.internal.asset.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Chi tiết tài sản và lịch sử (019 FR-012; api-019 {@code AssetDetail}, chủ dự án duyệt 2026-11-10). Lần bảo trì, hư hỏng
 * kèm dấu đã đính chính, đã Hủy ghi nhận (000 FR-028, FR-029; đính chính chỉ ghi chú, không đổi hiện hành).
 */
public record AssetDetails(
        AssetRow row,
        String location,
        List<String> allowedCommands,
        List<LocationPeriod> locations,
        List<DepartmentTransfer> departmentTransfers,
        List<MaintenanceEntry> maintenances,
        List<DamageEntry> damages) {

    public record LocationPeriod(
            String type, Long id, Instant from, Instant to, String command, Long actorStaffId, String reason) {}

    public record DepartmentTransfer(
            Long fromDepartmentId, Long toDepartmentId, Instant at, Long actorStaffId, String reason) {}

    public record MaintenanceEntry(
            long id,
            String kind,
            String result,
            LocalDate date,
            String content,
            String performedBy,
            Boolean outsourced,
            Long cost,
            Long procurementRequestId,
            Instant startedAt,
            Instant confirmedAt,
            boolean corrected,
            boolean voided) {}

    public record DamageEntry(
            long id,
            String description,
            String source,
            String impactLevel,
            Boolean awaitingTransfer,
            String removalReason,
            String repairDescription,
            String repairedBy,
            Instant reportedAt,
            Instant confirmedAt,
            boolean corrected,
            boolean voided) {}
}
