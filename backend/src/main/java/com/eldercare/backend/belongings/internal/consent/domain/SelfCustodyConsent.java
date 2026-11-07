package com.eldercare.backend.belongings.internal.consent.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Nội dung đồng ý cho tự giữ ({@code dong_y_tu_giu} → {@code simple_record} loại {@link #KIND}, R-15, data-model mục
 * 0; FR-015a, FR-017b). Trạng thái ở cột {@code status} ({@code EFFECTIVE}, {@code WITHDRAWN}, {@code EXPIRED}); một bản
 * Hiệu lực mỗi đồ qua {@code unique_key} (BR-M12-06), nhả khi rút hoặc hết hiệu lực.
 */
public record SelfCustodyConsent(
        long belongingId,
        long representativeId,
        String method,
        Long signedFormFileId,
        long recordedBy,
        Instant recordedAt,
        Long withdrawnByRepresentativeId,
        Long withdrawalSignedFormFileId,
        String withdrawalReason,
        Long withdrawnBy,
        Instant withdrawnAt,
        Long rootConsentId) {

    public static final SharedKind<SelfCustodyConsent> KIND =
            SharedKind.of("M12.SELF_CUSTODY_CONSENT", SelfCustodyConsent.class);

    public static final String EFFECTIVE = "EFFECTIVE";
    public static final String WITHDRAWN = "WITHDRAWN";
    public static final String EXPIRED = "EXPIRED";

    /** Cách ghi (FR-017b): qua cổng, bản ký scan do Hành chính ghi, ký trong lệnh tiếp nhận (FR-005). */
    public static final String PORTAL = "PORTAL";

    public static final String SIGNED_FORM = "SIGNED_FORM";
    public static final String IN_INTAKE_COMMAND = "IN_INTAKE_COMMAND";

    /**
     * Phần tách kế thừa đồng ý của đồ bị tách (013-3, FR-012): bản riêng của phần đó, {@code rootConsentId} trỏ bản gốc
     * để rút đồng ý thì hết cho mọi phần; mỗi phần hết hiệu lực riêng khi sang trạng thái kết thúc.
     */
    public static final String INHERITED = "INHERITED";

    /** Mã bản gốc của nhóm đồng ý mà bản này thuộc về. */
    public long rootOf(long ownId) {
        return rootConsentId == null ? ownId : rootConsentId;
    }

    /** Khóa duy nhất của bản Hiệu lực cho một đồ. */
    public static String effectiveKey(long belongingId) {
        return KIND.code() + ":" + belongingId;
    }

    public SelfCustodyConsent withdrawn(
            Long representativeId, Long signedFormFileId, String reason, long by, Instant at) {
        return new SelfCustodyConsent(
                belongingId,
                this.representativeId,
                method,
                this.signedFormFileId,
                recordedBy,
                recordedAt,
                representativeId,
                signedFormFileId,
                reason,
                by,
                at,
                rootConsentId);
    }
}
