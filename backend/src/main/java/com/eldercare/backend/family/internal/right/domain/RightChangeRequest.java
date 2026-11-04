package com.eldercare.backend.family.internal.right.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.notification.FamilyRight;

/**
 * Yêu cầu thay đổi quyền ({@code yeu_cau_quyen}; data-model mục 3, 0.1; BR-M10-07, FR-013, FR-020). Theo R-15 là một dòng
 * {@code simple_record} loại {@link #KIND}, trạng thái ở cột {@code status} ({@link Status}). US1 chỉ tạo yêu cầu Hiệu lực
 * ngay của phiếu đăng ký (FR-008); lập, xác nhận, duyệt, từ chối, hủy và {@code khoa_cho} thuộc US3 (T015, T016).
 *
 * @param confirmedByFamilyMemberId người đại diện xác nhận (cách {@code PORTAL}) hoặc ký bản ký ({@code SIGNED_FORM})
 * @param formId phiếu đăng ký khi nguồn là {@code REGISTRATION_FORM}
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
        String reason) {

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
}
