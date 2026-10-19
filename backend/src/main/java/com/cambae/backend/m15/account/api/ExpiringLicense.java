package com.cambae.backend.m15.account.api;

import java.time.LocalDate;

/** Giấy phép người hành nghề sắp hết hạn, cho cảnh báo FR-039. */
public record ExpiringLicense(long licenseId, long staffId, String name, LocalDate expiryDate) {}
