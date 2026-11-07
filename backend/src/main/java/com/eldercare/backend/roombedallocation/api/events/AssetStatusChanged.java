package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.roombedallocation.api.dto.AssetStatus;
import java.time.Instant;
import java.util.List;

/**
 * 019 contracts mục 4 (TaiSanDaDoiTrangThai) cho 016; thông báo của lệnh (FR-021) đi kèm. bedId rỗng khi không phải
 * tài sản giường; from rỗng khi ghi tăng.
 */
public record AssetStatusChanged(
        long assetId,
        long departmentId,
        Long bedId,
        AssetStatus from,
        AssetStatus to,
        String command,
        Instant at,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {
    public AssetStatusChanged {
        notificationRequests = List.copyOf(notificationRequests);
    }
}
