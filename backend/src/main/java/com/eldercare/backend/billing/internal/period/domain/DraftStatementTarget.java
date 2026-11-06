package com.eldercare.backend.billing.internal.period.domain;

/** Khi requiresAdjustment=true, caller phải đi luồng điều chỉnh; statement có thể chưa tồn tại. */
public record DraftStatementTarget(
        DraftCostStatement statement, boolean created, boolean reopened, boolean requiresAdjustment) {}
