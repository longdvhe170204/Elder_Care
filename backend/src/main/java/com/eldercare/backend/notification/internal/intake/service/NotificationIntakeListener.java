package com.eldercare.backend.notification.internal.intake.service;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Listener duy nhất cho mọi yêu cầu thông báo ({@code TiepNhanListener}; FR-001, research B-02, B-03): Spring giao theo
 * kiểu giao diện nên mọi sự kiện cài {@link HasNotificationRequests} tới đây sau commit của lệnh nguồn, qua outbox.
 * Lỗi bất ngờ làm sự kiện chưa hoàn tất và được giao lại; chống trùng của {@link NotificationIntake} giữ cho không gửi
 * trùng.
 */
@Component
class NotificationIntakeListener {

    private final NotificationIntake intake;

    NotificationIntakeListener(NotificationIntake intake) {
        this.intake = intake;
    }

    @ApplicationModuleListener
    void on(HasNotificationRequests event) {
        intake.receive(event);
    }
}
