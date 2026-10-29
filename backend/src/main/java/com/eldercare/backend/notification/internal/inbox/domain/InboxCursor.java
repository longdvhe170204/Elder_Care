package com.eldercare.backend.notification.internal.inbox.domain;

import java.time.Instant;

/** Vị trí sau dòng cuối của trang trước, theo thứ tự (Khẩn cấp chưa xác nhận, thời điểm gửi, id) giảm dần. */
public record InboxCursor(int urgentUnacknowledged, Instant sentAt, long id) {}
