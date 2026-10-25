package com.cambae.backend.m02.api.events;

import java.time.LocalDate;

/** Phiên bản đơn giá đã tạo, kể cả hiệu lực lùi (DBR-08); 010 tính lại khoản thiếu đơn giá (010 FR-012). */
public record PriceVersionCreated(long serviceId, LocalDate effectiveFrom, LocalDate effectiveTo) {}
