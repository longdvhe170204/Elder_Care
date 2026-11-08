package com.eldercare.backend.roombedallocation.internal.asset.domain;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.roombedallocation.api.dto.AssetStatus;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 019 T020 (FR-015, Q-242, Q-245, Q-256): quy tắc tài sản giường. Hỏng, Đang bảo trì đi cùng giường Đang bảo trì; Sẵn
 * sàng đi cùng giường theo phân bổ; Đã thanh lý đi cùng giường Không sử dụng. Tài sản giường không dùng Đang sử dụng.
 */
public final class BedAssetRules {
    private BedAssetRules() {}

    /** Lệnh giường của 003 khi tài sản đổi {@code from} → {@code to}; rỗng khi giường giữ nguyên trạng thái. */
    public static Optional<BedAssetCommand> bedCommand(AssetStatus from, AssetStatus to) {
        if (to == AssetStatus.DISPOSED) return Optional.of(BedAssetCommand.RETIREMENT);
        boolean wasMaintenance = from == AssetStatus.DAMAGED || from == AssetStatus.UNDER_MAINTENANCE;
        boolean isMaintenance = to == AssetStatus.DAMAGED || to == AssetStatus.UNDER_MAINTENANCE;
        if (from == AssetStatus.READY && isMaintenance) return Optional.of(BedAssetCommand.MAINTENANCE);
        if (wasMaintenance && to == AssetStatus.READY) return Optional.of(BedAssetCommand.READY);
        return Optional.empty();
    }

    /**
     * Q-245: Khẩn cấp cho Trưởng tầng và Người phụ trách ca của tầng; Q-256: thêm Quản lý viện, Hành chính khi tầng
     * không còn giường Trống đạt FR-018 cho người đang nằm.
     */
    public static List<RecipientGroup> unsafeRecipients(long floorId, List<LocationView> suitable) {
        var recipients = new ArrayList<RecipientGroup>(List.of(
                new RecipientGroup.FloorManager(floorId, new Substitution.Default()),
                new RecipientGroup.ShiftLead(floorId, new Substitution.Default())));
        if (suitable.stream().noneMatch(b -> Objects.equals(b.floorId(), floorId))) {
            recipients.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
            recipients.add(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
        }
        return List.copyOf(recipients);
    }

    /** Q-256: danh sách giường Trống đạt điều kiện ở tầng khác; rỗng thì ghi "không còn giường phù hợp trong viện". */
    public static String otherFloorBeds(long floorId, List<LocationView> suitable) {
        var others = suitable.stream()
                .filter(b -> !Objects.equals(b.floorId(), floorId))
                .map(b -> b.floorName() + ", " + b.roomName() + ", " + b.bedCode())
                .collect(Collectors.joining("; "));
        return others.isEmpty() ? "không còn giường phù hợp trong viện" : others;
    }
}
