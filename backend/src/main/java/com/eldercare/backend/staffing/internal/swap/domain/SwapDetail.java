package com.eldercare.backend.staffing.internal.swap.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Chi tiết đổi ca ở {@code simple_record} loại {@code M09.SWAP_DETAIL} (data-model mục 0.1: {@code yeu_cau_doi_ca} →
 * {@code ho_so_chung}). Trạng thái nằm ở yêu cầu phê duyệt và được chép sang cột {@code status} của dòng.
 */
public record SwapDetail(
        long requestId,
        SwapKind kind,
        long giverStaffId,
        long shiftXId,
        long recipientStaffId,
        Long shiftYId,
        String reason,
        String recipientDeclineReason,
        Long approverAccountId,
        Long leadForX,
        Long leadForY,
        boolean selfApproved,
        Instant earliestStart,
        Instant remindedAt,
        Instant decidedAt) {

    public static final SharedKind<SwapDetail> KIND = SharedKind.of("M09.SWAP_DETAIL", SwapDetail.class);

    public static String uniqueKey(long requestId) {
        return "M09.SWAP:" + requestId;
    }

    public SwapDetail declined(String declineReason) {
        return new SwapDetail(
                requestId,
                kind,
                giverStaffId,
                shiftXId,
                recipientStaffId,
                shiftYId,
                reason,
                declineReason,
                approverAccountId,
                leadForX,
                leadForY,
                selfApproved,
                earliestStart,
                remindedAt,
                decidedAt);
    }

    public SwapDetail decided(long approver, Long x, Long y, boolean self) {
        return new SwapDetail(
                requestId,
                kind,
                giverStaffId,
                shiftXId,
                recipientStaffId,
                shiftYId,
                reason,
                recipientDeclineReason,
                approver,
                x,
                y,
                self,
                earliestStart,
                remindedAt,
                decidedAt);
    }

    public SwapDetail reminded(Instant at) {
        return new SwapDetail(
                requestId,
                kind,
                giverStaffId,
                shiftXId,
                recipientStaffId,
                shiftYId,
                reason,
                recipientDeclineReason,
                approverAccountId,
                leadForX,
                leadForY,
                selfApproved,
                earliestStart,
                at,
                decidedAt);
    }

    public SwapDetail closed(Instant at) {
        return new SwapDetail(
                requestId,
                kind,
                giverStaffId,
                shiftXId,
                recipientStaffId,
                shiftYId,
                reason,
                recipientDeclineReason,
                approverAccountId,
                leadForX,
                leadForY,
                selfApproved,
                earliestStart,
                remindedAt,
                at);
    }
}
