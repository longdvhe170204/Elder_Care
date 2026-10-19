package com.cambae.backend.m15.account.api.events;

import java.time.LocalDate;
import java.util.List;

/** Giấy phép cơ sở hoặc người hành nghề sắp hết hạn; báo Quản lý viện (FR-039; 002 research A-09). */
public record LicenseExpiring(
        String licenseKind,
        long licenseId,
        LocalDate expiryDate,
        List<String> affectedActivities,
        List<Long> relatedStaffIds) {}
