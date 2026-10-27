package com.cambae.backend.m02.internal.registration.domain;

/**
 * Mốc phát sinh nghĩa vụ khai báo (data-model mục 4; glossary 24.5): Hoàn tất tiếp nhận (FR-001), phần nội trú có hiệu lực
 * (FR-001a), phụ lục gia hạn (FR-007), khai báo Hết hạn (FR-006).
 */
public enum ObligationTrigger {
    ADMISSION_COMPLETED,
    RESIDENTIAL_EFFECTIVE,
    RENEWAL_ADDENDUM,
    EXPIRED
}
