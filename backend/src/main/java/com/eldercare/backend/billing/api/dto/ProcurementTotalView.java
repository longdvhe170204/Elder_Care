package com.eldercare.backend.billing.api.dto;

/** Tổng chi mua sắm theo bộ phận, trạng thái (016 FR-055a); số tiền theo đồng. */
public record ProcurementTotalView(
        long departmentId, ProcurementStatus status, long requestCount, long approvedAmount, long actualAmount) {}
