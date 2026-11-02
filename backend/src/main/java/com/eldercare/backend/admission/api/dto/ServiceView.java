package com.eldercare.backend.admission.api.dto;

/** Dịch vụ trong danh mục (FR-034); {@code unit} là đơn vị tính hiển thị. */
public record ServiceView(long id, ServiceType type, String name, String unit, BillingPeriod billingPeriod) {}
