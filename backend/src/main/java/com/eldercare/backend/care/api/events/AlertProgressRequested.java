package com.eldercare.backend.care.api.events;

/**
 * 014 research L-11 (YeuCauGhiDienBienCanhBao, chủ dự án duyệt 2026-11-05 đặt ở care): 007 ghi diễn biến vào cảnh báo
 * đang mở của nguồn; không đổi trạng thái, không đóng; không có cảnh báo mở thì bỏ qua.
 */
public record AlertProgressRequested(
        String sourceType, long sourceId, String progressCode, String content, String eventKey) {}
