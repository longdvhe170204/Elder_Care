package com.eldercare.backend.incident.internal.incident.domain;

/**
 * Người cao tuổi tìm được khi báo khẩn (đề xuất 2026-11-23): chỉ họ tên, mã hồ sơ, trạng thái, phòng và tầng đang ở để
 * người báo chọn đúng người và người nhận biết nơi; không phải hồ sơ.
 */
public record IncidentResident(
        long id,
        String fullName,
        String profileCode,
        String status,
        String roomName,
        Long floorAreaId,
        String floorName) {}
