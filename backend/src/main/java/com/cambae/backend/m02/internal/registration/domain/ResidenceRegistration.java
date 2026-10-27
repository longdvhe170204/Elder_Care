package com.cambae.backend.m02.internal.registration.domain;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Khai báo tạm trú, lưu trú (FR-003 → FR-005; data-model mục 3), phần US1 cần: loại, gia hạn hay không, nghĩa vụ đáp ứng,
 * hạn khai báo, xác nhận thường trú cùng xã/phường, trạng thái, ngày hết hạn.
 */
public record ResidenceRegistration(
        long id,
        long residentId,
        RegistrationType type,
        boolean renewal,
        Long obligationId,
        Instant dueAt,
        boolean localPermanentResident,
        String basisNote,
        RegistrationStatus status,
        LocalDate expiryDate) {}
