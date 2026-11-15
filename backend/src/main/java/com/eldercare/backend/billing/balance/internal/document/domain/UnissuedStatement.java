package com.eldercare.backend.billing.balance.internal.document.domain;

import java.time.Instant;

/** Bảng chi phí đã có giao dịch thanh toán nhưng chưa phát hành phiếu: mã bảng, người cao tuổi, lúc xác nhận thanh toán. */
public record UnissuedStatement(long costStatementId, long residentId, Instant paidAt) {}
