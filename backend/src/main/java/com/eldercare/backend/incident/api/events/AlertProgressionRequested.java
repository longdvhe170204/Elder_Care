package com.eldercare.backend.incident.api.events;

/** Sự kiện/yêu cầu công khai theo hợp đồng 007 mục 1.12–1.13; không trực tiếp gửi thông báo. */
public record AlertProgressionRequested(
        String sourceType, long sourceId, String progressionTypeCode, String content, String eventKey) {}
