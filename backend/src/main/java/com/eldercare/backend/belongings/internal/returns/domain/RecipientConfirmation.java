package com.eldercare.backend.belongings.internal.returns.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Nội dung xác nhận người nhận khác ({@code xac_nhan_nguoi_nhan} → {@code simple_record} loại {@link #KIND}, R-15,
 * data-model mục 0; FR-017, FR-017a, FR-017b, BR-M12-04). Trạng thái ở cột {@code status}; người nhận và danh sách đồ
 * không đổi sau khi lập (chỉ {@link #excluded} thêm khi đồ rời trạng thái hợp lệ).
 */
public record RecipientConfirmation(
        String recipientName,
        String recipientIdDocument,
        boolean recipientIsResident,
        List<Long> belongingIds,
        List<Long> excludedBelongingIds,
        long createdBy,
        long createdByStaffId,
        Instant createdAt,
        String method,
        Long signedRepresentativeId,
        Long signedFormFileId,
        Long decidedByAccountId,
        Long decidedByRepresentativeId,
        Instant effectiveAt,
        Instant expiresAt,
        String reason,
        String onBehalfBasis,
        Instant managerNotifiedAt,
        Long usedByHandoverId) {

    public static final SharedKind<RecipientConfirmation> KIND =
            SharedKind.of("M12.RECIPIENT_CONFIRMATION", RecipientConfirmation.class);

    /** Trạng thái (api-013 {@code RecipientConfirmation.status}). */
    public static final String PENDING_CONFIRMATION = "PENDING_CONFIRMATION";

    public static final String EFFECTIVE = "EFFECTIVE";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String USED = "USED";
    public static final String EXPIRED = "EXPIRED";

    /** Cách xác nhận (FR-017): qua cổng, bản ký scan do Hành chính ghi, Quản lý viện duyệt thay. */
    public static final String PORTAL = "PORTAL";

    public static final String SIGNED_FORM = "SIGNED_FORM";
    public static final String MANAGER_ON_BEHALF = "MANAGER_ON_BEHALF";

    /** Đồ còn trong danh sách (chưa bị loại). */
    public List<Long> remaining() {
        List<Long> remaining = new ArrayList<>(belongingIds);
        remaining.removeAll(excludedBelongingIds);
        return remaining;
    }

    public RecipientConfirmation withExcluded(long belongingId) {
        List<Long> excluded = new ArrayList<>(excludedBelongingIds);
        excluded.add(belongingId);
        return copy(
                excluded,
                method,
                decidedByAccountId,
                decidedByRepresentativeId,
                effectiveAt,
                expiresAt,
                reason,
                onBehalfBasis,
                managerNotifiedAt,
                usedByHandoverId);
    }

    /** Hiệu lực từ {@code at} đến {@code expiresAt} (CFG-M10-08). */
    public RecipientConfirmation effective(
            String method,
            Long accountId,
            Long representativeId,
            Instant at,
            Instant expiresAt,
            String basis,
            String reason) {
        return copy(
                excludedBelongingIds,
                method,
                accountId,
                representativeId,
                at,
                expiresAt,
                reason,
                basis,
                managerNotifiedAt,
                usedByHandoverId);
    }

    /** Ghi lý do khi Từ chối, Hủy, hoặc người dùng/hệ thống chuyển trạng thái cuối. */
    public RecipientConfirmation closed(Long accountId, Long representativeId, String reason) {
        return copy(
                excludedBelongingIds,
                method,
                accountId,
                representativeId,
                effectiveAt,
                expiresAt,
                reason,
                onBehalfBasis,
                managerNotifiedAt,
                usedByHandoverId);
    }

    public RecipientConfirmation used(long handoverId) {
        return copy(
                excludedBelongingIds,
                method,
                decidedByAccountId,
                decidedByRepresentativeId,
                effectiveAt,
                expiresAt,
                reason,
                onBehalfBasis,
                managerNotifiedAt,
                handoverId);
    }

    public RecipientConfirmation managerNotified(Instant at) {
        return copy(
                excludedBelongingIds,
                method,
                decidedByAccountId,
                decidedByRepresentativeId,
                effectiveAt,
                expiresAt,
                reason,
                onBehalfBasis,
                at,
                usedByHandoverId);
    }

    private RecipientConfirmation copy(
            List<Long> excluded,
            String method,
            Long accountId,
            Long representativeId,
            Instant effectiveAt,
            Instant expiresAt,
            String reason,
            String basis,
            Instant managerNotifiedAt,
            Long usedByHandoverId) {
        return new RecipientConfirmation(
                recipientName,
                recipientIdDocument,
                recipientIsResident,
                belongingIds,
                excluded,
                createdBy,
                createdByStaffId,
                createdAt,
                method,
                signedRepresentativeId,
                signedFormFileId,
                accountId,
                representativeId,
                effectiveAt,
                expiresAt,
                reason,
                basis,
                managerNotifiedAt,
                usedByHandoverId);
    }
}
