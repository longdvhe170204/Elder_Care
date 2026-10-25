package com.cambae.backend.m02.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Phiên bản đơn giá hiệu lực (FR-035); {@code effectiveTo} rỗng khi còn mở. */
public record UnitPriceView(
        long serviceId, long priceVersionId, BigDecimal unitPrice, LocalDate effectiveFrom, LocalDate effectiveTo) {}
