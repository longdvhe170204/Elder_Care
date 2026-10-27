package com.cambae.backend.m02.internal.registration.domain;

import java.time.Instant;

/**
 * Nghĩa vụ khai báo (kỹ thuật, đệm số liệu tính; data-model mục 4, research Z-03): mốc, hạn chốt lúc ghi mốc, khai báo đáp
 * ứng nếu đã lập, đã ghi nộp hay chưa, đã báo Quản lý viện quá hạn hay chưa.
 */
public record RegistrationObligation(
        long id,
        long residentId,
        ObligationTrigger trigger,
        Instant triggerAt,
        Instant dueAt,
        long sourceId,
        Long registrationId,
        boolean submitted,
        Instant managerNotifiedAt) {}
