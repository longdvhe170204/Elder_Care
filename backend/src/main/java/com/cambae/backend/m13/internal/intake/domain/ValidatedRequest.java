package com.cambae.backend.m13.internal.intake.domain;

import com.cambae.backend.common.notification.ContentPart;
import com.cambae.backend.common.notification.InformationSubject;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Yêu cầu sau kiểm tra: mức, phần nội dung, đối tượng thông tin đã bổ sung mặc định; dấu; lý do khi bị từ chối.
 *
 * @param content phần nội dung, phần thiếu loại thông tin đã thành {@code HEALTH}
 */
public record ValidatedRequest(
        NotificationRequest request,
        IntakeResult result,
        Optional<String> rejectionReason,
        NotificationLevel level,
        List<ContentPart> content,
        Optional<InformationSubject> informationSubject,
        Set<RequestFlag> flags) {}
