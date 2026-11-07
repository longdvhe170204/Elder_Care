package com.eldercare.backend.notification.web.response;

import com.eldercare.backend.notification.internal.inbox.domain.InboxItem;
import com.eldercare.backend.notification.internal.inbox.service.InboxService.Page;
import java.util.List;
import java.util.function.Predicate;

/** Một trang hộp thông báo ({@code GET /me/notifications}, contracts/api-009.yaml). */
public record InboxBody(
        int unreadCount, int unacknowledgedUrgentCount, List<NotificationBody> items, String nextCursor) {

    /** {@code canTakeCharge}: nút "Xác nhận và tiếp nhận" của từng dòng (FR-026a; app hiện nút ngay trên danh sách). */
    public static InboxBody of(Page page, Predicate<InboxItem> canTakeCharge) {
        return new InboxBody(
                page.unreadCount(),
                page.unacknowledgedUrgentCount(),
                page.items().stream()
                        .map(o -> NotificationBody.of(o, canTakeCharge.test(o.row())))
                        .toList(),
                page.nextCursor().orElse(null));
    }
}
