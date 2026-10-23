package com.cambae.backend.m07.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import java.time.Instant;
import java.util.List;

/**
 * Sự kiện yêu cầu cảnh báo y tế về thuốc (YeuCauCanhBao).
 * Module M05 (007) và M13 (009) lắng nghe để xử lý cảnh báo và gửi thông báo.
 */
public record MedicationAlertRequestedEvent(
        String alertType, // BO_LO_LIEU | PHAN_UNG_THUOC | TU_CHOI_LIEN_TIEP | XUNG_DOT_DI_UNG | ...
        String alertLevel, // NHE | TRUNG_BINH | KHAN_CAP
        long residentId,
        String sourceRecord,
        String deduplicationKey,
        Instant occurredAt,
        List<NotificationRequest> notificationRequests)
        implements HasNotificationRequests {}
