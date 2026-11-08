package com.eldercare.backend.health.internal.threshold.domain;

/** Phiên bản ngưỡng kèm lý do thiết lập và người thiết lập, để hiển thị lịch sử (FR-020, FR-080). */
public record ThresholdListing(ThresholdVersion version, String reason, long createdBy) {}
