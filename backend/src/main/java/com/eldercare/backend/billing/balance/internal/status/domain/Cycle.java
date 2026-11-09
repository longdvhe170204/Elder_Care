package com.eldercare.backend.billing.balance.internal.status.domain;

import java.time.LocalDate;

/** Chu kỳ báo của sổ (data-model 017 mục 2): đầu chu kỳ CFG-M11-05, lần báo trước, ngày bắt đầu còn nợ (FR-019). */
public record Cycle(LocalDate cycleStartedOn, LocalDate lastNoticeOn, LocalDate inDebtSince) {

    public static final Cycle NONE = new Cycle(null, null, null);
}
