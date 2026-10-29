package com.eldercare.backend.administration.account.api.spi;

import java.time.LocalDate;
import java.util.Set;

/** Giấy phép còn hiệu lực của nhân viên kèm các mục phạm vi hành nghề (FR-037, FR-038; 008 FR-006). */
public record LicenseValidity(long licenseId, String name, LocalDate expiryDate, Set<Long> practiceScopeIds) {}
