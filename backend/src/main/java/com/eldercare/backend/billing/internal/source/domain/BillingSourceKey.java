package com.eldercare.backend.billing.internal.source.domain;

/** Khóa nội bộ theo id nguồn và đơn vị tính phí; loại nguồn được caller giới hạn riêng. */
public record BillingSourceKey(String sourceId, String billingUnit) {}
