package com.eldercare.backend.health.api.dto;

/** Nhãn của bản ghi chỉ số; không thay thế trạng thái xác nhận. */
public enum VitalSignLabel {
    IMMEDIATE_ACTION,
    NOT_REMEASURED,
    LATE_RECORDED,
    OFFLINE_RECORDED
}
