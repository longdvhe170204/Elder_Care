package com.cambae.backend.m02.internal.servicecatalog.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Phiên bản đơn giá (bảng {@code price_version}); {@code effectiveTo} rỗng khi còn mở, tính cả ngày cuối. */
public record PriceVersion(
        long id,
        long serviceId,
        BigDecimal unitPrice,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean referenced) {}
