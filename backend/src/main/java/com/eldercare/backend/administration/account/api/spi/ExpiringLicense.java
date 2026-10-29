package com.eldercare.backend.administration.account.api.spi;

import java.time.LocalDate;

/**
 * Giấy phép người hành nghề sắp hết hạn, cho cảnh báo FR-039. {@code licenseType} (bổ sung 2026-10-21, T066) để cảnh
 * báo nêu được nghiệp vụ chuyên môn sẽ bị tắt.
 */
public record ExpiringLicense(long licenseId, long staffId, String licenseType, String name, LocalDate expiryDate) {}
