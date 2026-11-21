package com.eldercare.backend.admission.internal.change.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Lần Hành chính gửi yêu cầu thay đổi lưu trú ở Nháp cho người đại diện xác nhận trên ứng dụng (FR-043c, D đề nghị
 * 2026-11-20; {@code simple_record} loại {@link #KIND}, đối tượng cha là yêu cầu phê duyệt). Trạng thái ở cột
 * {@code status}: Chờ người đại diện → Đồng ý / Không đồng ý; sửa nháp hoặc gửi lại thì lần đang chờ hoặc đã đồng ý thành
 * Bị thay thế. Đồng ý thay cho xác nhận kèm bằng chứng ({@link RepresentativeConfirmation}).
 */
public record ChangeConfirmationRequest(
        long requestId,
        long sentBy,
        Instant sentAt,
        Long respondedByFamilyMemberId,
        Long respondedByAccountId,
        Instant respondedAt,
        String declineReason,
        Instant remindedAt) {

    public static final SharedKind<ChangeConfirmationRequest> KIND =
            SharedKind.of("M02.CHANGE_CONFIRMATION", ChangeConfirmationRequest.class);

    /** Đối tượng cha: yêu cầu thay đổi lưu trú (yêu cầu phê duyệt loại M02.RESIDENCY_CHANGE). */
    public static final String PARENT_TYPE = "M02.RESIDENCY_CHANGE";

    public static final String AWAITING = "AWAITING";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String DECLINED = "DECLINED";
    public static final String SUPERSEDED = "SUPERSEDED";

    public static ChangeConfirmationRequest sent(long requestId, long sentBy, Instant at) {
        return new ChangeConfirmationRequest(requestId, sentBy, at, null, null, null, null, null);
    }

    public ChangeConfirmationRequest answered(long familyMemberId, long accountId, Instant at, String reason) {
        return new ChangeConfirmationRequest(
                requestId, sentBy, sentAt, familyMemberId, accountId, at, reason, remindedAt);
    }

    public ChangeConfirmationRequest reminded(Instant at) {
        return new ChangeConfirmationRequest(
                requestId,
                sentBy,
                sentAt,
                respondedByFamilyMemberId,
                respondedByAccountId,
                respondedAt,
                declineReason,
                at);
    }
}
