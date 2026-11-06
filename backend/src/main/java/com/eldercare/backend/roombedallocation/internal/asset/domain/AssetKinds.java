package com.eldercare.backend.roombedallocation.internal.asset.domain;

import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.LocalDate;

/**
 * 019 data-model mục 0.1 (R-15; glossary 25.7): loại dòng ở bảng chung của tài sản. Loại tài sản có hai mã hệ thống
 * {@link #BED_CODE}, {@link #VEHICLE_CODE} nạp ở V019_05 (chủ dự án duyệt 2026-11-06).
 */
public final class AssetKinds {
    public static final String ENTITY = "M03.ASSET";
    public static final String BED_CODE = "BED";
    public static final String VEHICLE_CODE = "VEHICLE";

    /** Thuộc tính mục loại: {@code system} đúng với Giường, Xe. */
    public record TypeAttributes(Boolean system) {}

    /** Vị trí của một khoảng lịch sử vị trí (tai_san_vi_tri). */
    public record Location(String type, Long id, String reason) {}

    /** Lần bảo trì (tai_san_bao_tri): mở khi đưa vào bảo trì (nháp), kết quả đặt một lần khi xác nhận. */
    public record Maintenance(
            String kind,
            String result,
            LocalDate date,
            String content,
            String performedBy,
            Boolean outsourced,
            Long cost,
            Long procurementRequestId) {}

    /**
     * Hư hỏng (tai_san_hu_hong): nguồn DAMAGE_REPORT, BED_PREPARATION; với giường có người: mức ảnh hưởng, dấu chờ chuyển
     * người (bản nháp tới khi gỡ dấu), lần gỡ dấu kèm lý do, nội dung đã sửa, người sửa đặt một lần (Q-245, Q-265).
     */
    public record Damage(
            String description,
            String source,
            String impactLevel,
            Boolean awaitingTransfer,
            String removalReason,
            String repairDescription,
            String repairedBy) {
        public Damage(String description, String source, String impactLevel, Boolean awaitingTransfer) {
            this(description, source, impactLevel, awaitingTransfer, null, null, null);
        }
    }

    public static final SharedKind<TypeAttributes> ASSET_TYPE =
            SharedKind.of("M03.ASSET_TYPE", TypeAttributes.class).notDeletable();
    public static final HistoryTrack<Location> LOCATION = new HistoryTrack<>(ENTITY, "LOCATION", Location.class);
    public static final HistoryTrack<Void> DEPARTMENT = new HistoryTrack<>(ENTITY, "DEPARTMENT", Void.class);
    public static final SharedKind<Maintenance> MAINTENANCE = SharedKind.of("M03.ASSET_MAINTENANCE", Maintenance.class);
    public static final SharedKind<Damage> DAMAGE = SharedKind.of("M03.ASSET_DAMAGE", Damage.class);

    private AssetKinds() {}
}
