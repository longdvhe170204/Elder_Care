package com.eldercare.backend.billing.balance.internal.settlement.domain;

/** Bản scan có chữ ký của người đại diện gắn vào một phiên bản bảng quyết toán (017 FR-034; data-model mục 6). */
public record SignedCopyRecord(long statementId, long fileId) {}
