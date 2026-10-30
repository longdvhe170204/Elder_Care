package com.eldercare.backend.notification.internal.urgent.domain;

import com.eldercare.backend.notification.api.dto.RecipientType;

/** Thông báo Khẩn cấp chưa xác nhận cần lượt nhắc gọi (FR-027, FR-028). */
public record DueNotification(
        long notificationId,
        RecipientType calleeType,
        long calleeId,
        Long residentId,
        Long floorAreaId,
        String sourceRecordType,
        String sourceRecordId,
        boolean deliveryFailed) {}
