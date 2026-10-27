package com.cambae.backend.m04.api.events;

import com.cambae.backend.common.notification.*;
import java.util.*;

/** C-13: nguồn gọi truyền nhóm nhận theo nghiệp vụ; khuôn chỉ chứa tiêu đề chung không có dữ liệu sức khỏe. */
public interface CareNotice extends CareEvent, HasNotificationRequests {
    NotificationLevel level();

    default Optional<Long> notificationResidentId() {
        return Optional.empty();
    }

    @Override
    default List<NotificationRequest> notificationRequests() {
        return List.of(CareEventNotifications.create(this, level(), context().recipients(), eventKey()));
    }
}
