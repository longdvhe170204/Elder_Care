package com.eldercare.backend.notification.internal.lookup.domain;

import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.notification.api.dto.AcknowledgementMethod;
import com.eldercare.backend.notification.api.dto.NotificationStatus;
import com.eldercare.backend.notification.api.dto.RecipientType;
import java.time.Instant;
import java.util.List;

/** Một thông báo (một người nhận của một yêu cầu) với dữ liệu của yêu cầu. */
public record SentNotification(
        long notificationId,
        long requestId,
        String sourceGroup,
        String sourceRecordType,
        String sourceRecordId,
        NotificationLevel level,
        RecipientType recipientType,
        long recipientId,
        List<String> basis,
        InformationSubject informationSubject,
        Q354Case q354Case,
        Instant receivedAt,
        NotificationStatus status,
        Instant sentAt,
        Instant viewedAt,
        Instant acknowledgedAt,
        AcknowledgementMethod acknowledgementMethod,
        List<String> flags) {}
