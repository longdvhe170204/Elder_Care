package com.eldercare.backend.incident.internal.alert.domain;

/**
 * Chi tiết người bấm chọn sau khi gửi (FR-047i): mỗi trường đặt một lần. Lưu mỗi trường một dòng con
 * {@code M05.ASSIST_DETAIL} của cảnh báo, khóa duy nhất {@code ASSIST_DETAIL:<cảnh báo>:<trường>}.
 */
public record AssistDetails(AssistEventType eventType, Long floorAreaId, SosPlace place, Long residentId) {
    public boolean isEmpty() {
        return eventType == null && floorAreaId == null && place == null && residentId == null;
    }
}
