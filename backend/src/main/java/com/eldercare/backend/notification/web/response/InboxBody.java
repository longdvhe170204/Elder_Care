package com.eldercare.backend.notification.web.response;

import com.eldercare.backend.notification.internal.inbox.service.InboxService.Page;
import java.util.List;

/** Một trang hộp thông báo ({@code GET /me/notifications}, contracts/api-009.yaml). */
public record InboxBody(
        int unreadCount, int unacknowledgedUrgentCount, List<NotificationBody> items, String nextCursor) {

    public static InboxBody of(Page page) {
        return new InboxBody(
                page.unreadCount(),
                page.unacknowledgedUrgentCount(),
                page.items().stream().map(NotificationBody::of).toList(),
                page.nextCursor().orElse(null));
    }
}
