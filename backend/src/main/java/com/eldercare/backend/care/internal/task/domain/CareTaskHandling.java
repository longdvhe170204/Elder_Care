package com.eldercare.backend.care.internal.task.domain;

/** Chi tiết nhóm 3 trên status_history qua StatusHistoryApi, không tạo bảng lịch sử riêng. */
public record CareTaskHandling(String content, Long previousPerformerId, Long performerId, Long handoverId) {
    public CareTaskHandling(String content) {
        this(content, null, null, null);
    }
}
