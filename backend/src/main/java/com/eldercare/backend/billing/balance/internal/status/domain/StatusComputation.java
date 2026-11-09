package com.eldercare.backend.billing.balance.internal.status.domain;

import com.eldercare.backend.billing.api.dto.BalanceStatus;

/** Kết quả tính tình trạng (FR-016, FR-017): {@code daysCovered} rỗng khi còn nợ hoặc phí một ngày bằng 0, chưa biết. */
public record StatusComputation(BalanceStatus status, Integer daysCovered) {}
