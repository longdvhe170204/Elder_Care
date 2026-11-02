package com.eldercare.backend.billing.api.dto;

/** Giá trị theo hợp đồng api-010 đã dịch; không mở rộng danh sách nghiệp vụ. */
public enum PurchaseAssistanceStatus {
    DRAFT,
    AWAITING_CONSENT,
    PURCHASE_ALLOWED,
    REJECTED,
    CANCELLED,
    PURCHASED
}
