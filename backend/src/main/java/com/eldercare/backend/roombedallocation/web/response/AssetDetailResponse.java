package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.roombedallocation.api.dto.AssetStatus;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 019 FR-012: {@code AssetDetail} của api-019 (chủ dự án duyệt 2026-11-10): trường của {@code Asset} cộng lịch sử vị trí,
 * lần chuyển bộ phận, lần bảo trì, hư hỏng; lần bảo trì, hư hỏng kèm dấu đã đính chính, đã Hủy ghi nhận.
 */
@Schema(name = "AssetDetail")
public record AssetDetailResponse(
        Long id,
        String code,
        Long typeId,
        String name,
        Long departmentId,
        AssetStatus status,
        String location,
        Long bedId,
        List<String> flags,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Integer version,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) List<String> allowedCommands,
        List<Location> locations,
        List<DepartmentTransfer> departmentTransfers,
        List<Maintenance> maintenances,
        List<Damage> damages) {

    @Schema(name = "AssetLocationPeriod")
    public record Location(
            @Schema(allowableValues = {"AREA", "FLOOR", "ROOM", "PARKING_LOT", "ASSET_STORAGE"})
            String type,

            Long id,
            Instant from,
            Instant to,
            String command,
            Long actorStaffId,
            String reason) {}

    @Schema(name = "AssetDepartmentTransfer")
    public record DepartmentTransfer(
            Long fromDepartmentId, Long toDepartmentId, Instant at, Long actorStaffId, String reason) {}

    @Schema(name = "AssetMaintenance")
    public record Maintenance(
            Long id,

            @Schema(allowableValues = {"MAINTENANCE", "INSPECTION", "VEHICLE_INSPECTION", "INSURANCE"})
            String kind,

            @Schema(allowableValues = {"PASS", "FAIL"}) String result,
            LocalDate date,
            String content,
            String performedBy,
            Boolean outsourced,
            Long cost,
            Long procurementRequestId,
            Instant startedAt,
            Instant confirmedAt,
            Boolean corrected,
            Boolean voided) {}

    @Schema(name = "AssetDamage")
    public record Damage(
            Long id,
            String description,

            @Schema(allowableValues = {"DAMAGE_REPORT", "BED_PREPARATION"})
            String source,

            @Schema(allowableValues = {"UNSAFE", "STILL_SAFE"})
            String impactLevel,

            Boolean awaitingTransfer,
            String removalReason,
            String repairDescription,
            String repairedBy,
            Instant reportedAt,
            Instant confirmedAt,
            Boolean corrected,
            Boolean voided) {}

    public static AssetDetailResponse of(AssetDetails d) {
        var row = d.row();
        return new AssetDetailResponse(
                row.id(),
                row.code(),
                row.typeId(),
                row.name(),
                row.departmentId(),
                row.status(),
                d.location(),
                row.bedId(),
                row.flags().stream().map(Enum::name).toList(),
                row.version(),
                d.allowedCommands(),
                d.locations().stream()
                        .map(l -> new Location(
                                l.type(), l.id(), l.from(), l.to(), l.command(), l.actorStaffId(), l.reason()))
                        .toList(),
                d.departmentTransfers().stream()
                        .map(t -> new DepartmentTransfer(
                                t.fromDepartmentId(), t.toDepartmentId(), t.at(), t.actorStaffId(), t.reason()))
                        .toList(),
                d.maintenances().stream()
                        .map(m -> new Maintenance(
                                m.id(),
                                m.kind(),
                                m.result(),
                                m.date(),
                                m.content(),
                                m.performedBy(),
                                m.outsourced(),
                                m.cost(),
                                m.procurementRequestId(),
                                m.startedAt(),
                                m.confirmedAt(),
                                m.corrected(),
                                m.voided()))
                        .toList(),
                d.damages().stream()
                        .map(x -> new Damage(
                                x.id(),
                                x.description(),
                                x.source(),
                                x.impactLevel(),
                                x.awaitingTransfer(),
                                x.removalReason(),
                                x.repairDescription(),
                                x.repairedBy(),
                                x.reportedAt(),
                                x.confirmedAt(),
                                x.corrected(),
                                x.voided()))
                        .toList());
    }
}
