package com.eldercare.backend.notification.api.events;

import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.notification.api.dto.AcknowledgementMethod;

/** Thông báo Khẩn cấp đã được xác nhận ({@code ThongBaoKhanCapDaXacNhan}; FR-035, FR-044). */
public record UrgentNotificationAcknowledged(
        long notificationId, SourceRecord sourceRecord, AcknowledgementMethod method) {}
