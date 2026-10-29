package com.eldercare.backend.administration.account.api.spi;

import java.time.LocalDate;

/** Giấy phép cơ sở sắp hết hạn, cho cảnh báo FR-039. */
public record ExpiringFacilityLicense(long licenseId, String name, LocalDate expiryDate) {}
