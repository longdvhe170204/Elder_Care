package com.eldercare.backend.billing.balance.internal.status.domain;

import com.eldercare.backend.billing.api.dto.BalanceStatus;
import java.util.Set;

/** Tình trạng, chu kỳ sau lần tính và các thông báo phải gửi. */
public record Decision(BalanceStatus status, Cycle cycle, Set<Notice> notices) {}
