package com.cambae.backend.m13.internal.lookup.domain;

import com.cambae.backend.common.notification.InformationSubject;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.Q354Case;
import com.cambae.backend.m13.api.dto.AcknowledgementMethod;
import com.cambae.backend.m13.api.dto.NotificationStatus;
import com.cambae.backend.m13.api.dto.RecipientType;
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
