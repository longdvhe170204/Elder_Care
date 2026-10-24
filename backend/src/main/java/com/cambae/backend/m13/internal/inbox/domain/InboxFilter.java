package com.cambae.backend.m13.internal.inbox.domain;

import com.cambae.backend.common.notification.NotificationLevel;

/** Bộ lọc của {@code GET /me/notifications}. */
public record InboxFilter(NotificationLevel level, Boolean unread, String eventType, Long residentId) {}
