package com.cambae.backend.m02.api.dto;

/**
 * Bản ghi đón hợp lệ (012). {@code pickupType} là loại đón theo 012. Provisional: chữ ký view chưa được 012 (E) chốt.
 */
public record PickupRecordView(long id, long residentId, String pickupType) {}
