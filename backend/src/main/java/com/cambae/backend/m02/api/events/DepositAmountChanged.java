package com.cambae.backend.m02.api.events;

import java.math.BigDecimal;

/** Khoản đặt cọc của hợp đồng đổi; 017 nghe (FR-033a, FR-033b). */
public record DepositAmountChanged(long contractId, BigDecimal newAmount) {}
