package com.eldercare.backend.incident.api.dto;

import java.util.Optional;

/** Người thân hoặc người đi cùng thực tế đã vào thăm; người đi cùng có thể không có mã người thân. */
public record VisitTracePersonView(
        Optional<Long> familyMemberId,
        String fullName,
        Optional<String> relationship,
        Optional<String> phoneNumber,
        boolean hasNotificationChannel) {}
