package com.cambae.backend.m02.internal.servicecatalog.domain;

import java.math.BigDecimal;

/** Hợp đồng Hiệu lực có đơn giá đã ghi khác phiên bản mới nhất của dịch vụ (FR-036). */
public record MismatchedContract(
        long contractId, String contractNumber, BigDecimal recordedUnitPrice, BigDecimal newUnitPrice) {}
