package com.eldercare.backend.notification.internal.quiethours.domain;

import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.notification.api.dto.RecipientType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Thông báo đang Chờ hết giờ yên tĩnh cùng yêu cầu của nó, đủ để xét lại người nhận (FR-025). */
public record HeldNotification(
        long id,
        RecipientType recipientType,
        long recipientId,
        List<String> basis,
        NotificationLevel level,
        Long residentId,
        Optional<InformationSubject> informationSubject,
        boolean hasQ354Case,
        boolean invalidOutOfAppContent,
        boolean afterDeath,
        Set<InformationType> requestParts,
        Instant scheduledSendAt) {}
