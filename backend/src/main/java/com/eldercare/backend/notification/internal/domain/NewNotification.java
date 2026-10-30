package com.eldercare.backend.notification.internal.domain;

import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.notification.api.dto.RecipientType;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Thông báo mới cho một người nhận (FR-009 → FR-012). */
public record NewNotification(
        long requestId,
        RecipientType recipientType,
        long recipientId,
        Long accountId,
        List<String> basis,
        Set<InformationType> visibleParts,
        boolean identityOnly,
        NotificationLevel level,
        Instant createdAt,
        Q354Case q354Case) {}
