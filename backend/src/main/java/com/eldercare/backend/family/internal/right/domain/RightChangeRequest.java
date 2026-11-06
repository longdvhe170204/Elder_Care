package com.eldercare.backend.family.internal.right.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.notification.FamilyRight;
import java.time.Instant;

/**
 * Yêu cầu thay đổi quyền ({@code yeu_cau_quyen}; data-model mục 3, 0.1; BR-M10-07, FR-013, FR-020). Theo R-15 là một dòng
 * {@code simple_record} loại {@link #KIND}, trạng thái ở cột {@code status} ({@link Status}); {@code khoa_cho} là
 * {@code unique_key} khi Chờ xác nhận ({@link #pendingKey}), nhả khi có kết quả. Người lập, lúc lập là
 * {@code created_by}, {@code created_at} của dòng.
 *
 * @param confirmedByFamilyMemberId người đại diện xác nhận (cách {@code PORTAL}) hoặc ký bản ký ({@code SIGNED_FORM})
 * @param formId phiếu đăng ký khi nguồn là {@code REGISTRATION_FORM}
 * @param newPicker người đón mới chưa có hồ sơ: hồ sơ và quan hệ lập kèm lệnh lập (FR-013)
 * @param evidenceFileId bằng chứng đặt/thôi người đại diện (FR-005) hoặc bản scan bản ký (FR-015 b)
 * @param createdByFamilyMemberId người thân lập qua cổng; {@code null} khi lập tại quầy
 * @param appliedOnCreation tắt "được phép đón", tắt "được tự về" đã áp khi lập ({@code ap_ngay_khi_lap}; FR-017)
 * @param conflictingWishId mong muốn bị trái ({@code trai_mong_muon_id}; FR-020b)
 * @param decidedBy tài khoản xác nhận, duyệt, từ chối, hủy; {@code null} khi hệ thống hủy
 * @param discussedWithResident nội dung đã trao đổi với người cao tuổi khi Quản lý viện duyệt yêu cầu trái mong muốn
 */
public record RightChangeRequest(
        Type type,
        Long relationshipId,
        FamilyRight right,
        boolean newValue,
        Source source,
        ConfirmationMethod confirmationMethod,
        Long confirmedByFamilyMemberId,
        Long formId,
        String reason,
        NewPicker newPicker,
        Long evidenceFileId,
        Long createdByFamilyMemberId,
        boolean appliedOnCreation,
        Long conflictingWishId,
        Long decidedBy,
        Instant decidedAt,
        String decisionReason,
        String discussedWithResident) {

    public static final SharedKind<RightChangeRequest> KIND =
            SharedKind.of("M10.RIGHT_CHANGE_REQUEST", RightChangeRequest.class);

    public enum Type {
        RIGHT,
        SELF_RETURN,
        SET_REPRESENTATIVE,
        REMOVE_REPRESENTATIVE
    }

    public enum Status {
        PENDING_CONFIRMATION,
        EFFECTIVE,
        REJECTED,
        CANCELLED
    }

    public enum Source {
        COUNTER,
        PORTAL,
        REGISTRATION_FORM
    }

    public enum ConfirmationMethod {
        PORTAL,
        SIGNED_FORM,
        MANAGER_APPROVAL
    }

    public record NewPicker(
            String fullName, String phoneNumber, String idDocumentType, String idDocumentNumber, String relationship) {}

    /** Yêu cầu Hiệu lực ngay của phiếu đăng ký (FR-008). */
    public static RightChangeRequest ofRegistrationForm(
            long relationshipId, FamilyRight right, long signerId, long formId, Long conflictingWishId) {
        return new RightChangeRequest(
                Type.RIGHT,
                relationshipId,
                right,
                true,
                Source.REGISTRATION_FORM,
                conflictingWishId == null ? ConfirmationMethod.SIGNED_FORM : null,
                conflictingWishId == null ? signerId : null,
                formId,
                null,
                null,
                null,
                null,
                false,
                conflictingWishId,
                null,
                null,
                null,
                null);
    }

    /** {@code khoa_cho}: một yêu cầu Chờ xác nhận mỗi (người cao tuổi, quan hệ, quyền hoặc loại) (FR-018). */
    public String pendingKey(long residentId) {
        return "M10.RCR:" + residentId + ":" + (relationshipId == null ? 0 : relationshipId) + ":"
                + (right != null ? right.name() : type.name());
    }

    public RightChangeRequest confirmed(
            ConfirmationMethod method, Long familyMemberId, Long evidence, long by, Instant at) {
        return new RightChangeRequest(
                type,
                relationshipId,
                right,
                newValue,
                source,
                method,
                familyMemberId,
                formId,
                reason,
                newPicker,
                evidence != null ? evidence : evidenceFileId,
                createdByFamilyMemberId,
                appliedOnCreation,
                conflictingWishId,
                by,
                at,
                decisionReason,
                discussedWithResident);
    }

    public RightChangeRequest approved(long by, Instant at, String reason, String discussed) {
        return new RightChangeRequest(
                type,
                relationshipId,
                right,
                newValue,
                source,
                ConfirmationMethod.MANAGER_APPROVAL,
                null,
                formId,
                this.reason,
                newPicker,
                evidenceFileId,
                createdByFamilyMemberId,
                appliedOnCreation,
                conflictingWishId,
                by,
                at,
                reason,
                discussed);
    }

    /** Từ chối, hủy; {@code by} {@code null} khi hệ thống hủy. */
    public RightChangeRequest closed(Long by, Instant at, String reason) {
        return new RightChangeRequest(
                type,
                relationshipId,
                right,
                newValue,
                source,
                confirmationMethod,
                confirmedByFamilyMemberId,
                formId,
                this.reason,
                newPicker,
                evidenceFileId,
                createdByFamilyMemberId,
                appliedOnCreation,
                conflictingWishId,
                by,
                at,
                reason,
                discussedWithResident);
    }

    public RightChangeRequest withConflict(Long wishId) {
        return new RightChangeRequest(
                type,
                relationshipId,
                right,
                newValue,
                source,
                confirmationMethod,
                confirmedByFamilyMemberId,
                formId,
                reason,
                newPicker,
                evidenceFileId,
                createdByFamilyMemberId,
                appliedOnCreation,
                wishId,
                decidedBy,
                decidedAt,
                decisionReason,
                discussedWithResident);
    }
}
