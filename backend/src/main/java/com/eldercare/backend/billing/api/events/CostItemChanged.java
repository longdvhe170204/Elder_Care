package com.eldercare.backend.billing.api.events;

/** FR-038a: 017/016 đọc lại số liệu của người cao tuổi và kỳ; không mang danh sách khoản. */
public record CostItemChanged(long residentId, long periodId) {}
