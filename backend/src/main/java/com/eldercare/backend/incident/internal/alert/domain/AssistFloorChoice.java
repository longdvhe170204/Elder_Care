package com.eldercare.backend.incident.internal.alert.domain;

/**
 * Tầng xảy ra của yêu cầu hỗ trợ (đề xuất 2026-11-23): một tầng đang có ca, hoặc khu vực chung (sân, sảnh, cầu thang) thì
 * báo Người phụ trách ca của mọi tầng đang có ca như "chưa xác định tầng" của FR-047i. Không chọn gì thì lấy tầng theo ca.
 */
public record AssistFloorChoice(Long floorAreaId, boolean commonArea) {
    public static final AssistFloorChoice NONE = new AssistFloorChoice(null, false);

    public boolean isEmpty() {
        return floorAreaId == null && !commonArea;
    }
}
