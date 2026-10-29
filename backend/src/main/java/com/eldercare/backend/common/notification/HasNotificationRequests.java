package com.eldercare.backend.common.notification;

import java.util.List;

/**
 * Sự kiện miền của module nguồn cài giao diện này để gửi thông báo: 009 có một listener cho kiểu này, nên mọi module
 * dùng đúng một cách gửi (009 FR-001; 009 contracts/module-interfaces.md mục 1.1, research B-02; 000 T034).
 */
public interface HasNotificationRequests {

    /** Có thể rỗng; một sự kiện có thể sinh nhiều yêu cầu. */
    List<NotificationRequest> notificationRequests();
}
