package com.eldercare.backend.notification.internal.inbox.domain;

import com.eldercare.backend.common.notification.NotificationLevel;

/** Bộ lọc của {@code GET /me/notifications}. */
public record InboxFilter(NotificationLevel level, Boolean unread, String eventType, Long residentId) {}
