package com.eldercare.backend.incident.internal.critical.domain;

import java.time.Instant;

/**
 * Kết quả xác nhận lại nguyện vọng, bản ghi nhóm 3 gắn cảnh báo nguy kịch (FR-050c, FR-050g; R-15
 * {@code ket_qua_xac_nhan_lai} → {@code record_event}). Người ghi là {@code actor_id} của dòng.
 */
public record WishReconfirmationContent(
        Long contactedFamilyMemberId,
        String contactedName,
        ContactChannel channel,
        Instant contactedAt,
        ReconfirmationResult result,
        Long newWishVersionId) {}
