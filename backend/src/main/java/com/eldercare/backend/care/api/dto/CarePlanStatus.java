package com.eldercare.backend.care.api.dto;
/** T003: tên tương đương trạng thái/mã trong hợp đồng 005. */
public enum CarePlanStatus {
    DRAFT,
    PENDING_APPROVAL,
    PENDING_EFFECT,
    EFFECTIVE,
    EXPIRED,
    CANCELLED
}
