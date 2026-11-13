package com.eldercare.backend.roombedallocation.internal.allocation.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Chỉ mục yêu cầu nhập bù chờ duyệt (FR-027 (b), CHK051): {@code record_event} loại {@code M03.BED_BACKFILL_REQUEST} của
 * phân bổ hiện hành, {@code occurred_at} là lúc chuyển (gộp bảng {@code bed_backfill_request} 2026-11-13, research R-15
 * của 000).
 */
public record BackfillRequest(long approvalRequestId, long residentId, long newBedId) {

    public static final SharedKind<BackfillRequest> KIND =
            SharedKind.of("M03.BED_BACKFILL_REQUEST", BackfillRequest.class);
}
