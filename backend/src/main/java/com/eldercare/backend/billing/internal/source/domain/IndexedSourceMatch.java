package com.eldercare.backend.billing.internal.source.domain;

/** Kết quả chỉ mục nội bộ; không phải phản hồi HTTP hoặc số liệu ngày lưu trú tính từ 004. */
public record IndexedSourceMatch(IndexedBillingSource source, boolean cancelled, long version, Long existingItemId) {}
