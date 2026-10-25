package com.cambae.backend.m02.api.dto;

import java.math.BigDecimal;

/** Khoản đặt cọc cần có của hợp đồng (FR-030); {@code notRequired} thì {@code amount} bằng 0. */
public record DepositAmountView(long contractId, boolean notRequired, BigDecimal amount) {}
