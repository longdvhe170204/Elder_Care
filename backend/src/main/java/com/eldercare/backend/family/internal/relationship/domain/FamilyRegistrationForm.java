package com.eldercare.backend.family.internal.relationship.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.notification.FamilyRight;
import java.util.List;

/**
 * Phiếu đăng ký người thân ({@code phieu_dang_ky_nguoi_than}, dòng ở {@code phieu_dang_ky_dong}; data-model mục 2, 0.1;
 * FR-008, Q-126). Theo R-15 là một dòng {@code record_event} loại {@link #KIND} gắn hồ sơ người cao tuổi, dòng phiếu ở
 * {@code lines}; nhóm 3: ghi nháp, thêm yêu cầu của từng dòng rồi xác nhận trong cùng lệnh.
 *
 * @param representativeId người đại diện ký phiếu (hồ sơ người thân)
 * @param scanFileId bản scan, bắt buộc; tệp gắn chủ sở hữu {@link #KIND}
 */
public record FamilyRegistrationForm(long representativeId, long scanFileId, List<Line> lines) {

    public static final SharedKind<FamilyRegistrationForm> KIND =
            SharedKind.of("M10.FAMILY_REGISTRATION_FORM", FamilyRegistrationForm.class);

    public FamilyRegistrationForm {
        lines = List.copyOf(lines);
    }

    /** Một quyền trên phiếu; {@code requestId} là yêu cầu Hiệu lực tạo kèm (FR-020). */
    public record Line(long relationshipId, FamilyRight right, Long requestId) {}
}
