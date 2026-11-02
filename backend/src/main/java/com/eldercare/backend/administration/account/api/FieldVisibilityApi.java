package com.eldercare.backend.administration.account.api;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;

/** Quyền xem theo nhóm thông tin (mục 19.3; FR-049, BR-M15-11). 000 khai báo trước (000 T025, contracts/module-interfaces.md mục 2); feature 002 cài bản thật. */
public interface FieldVisibilityApi {

    Visibility canView(CurrentUser user, InfoGroup group, Target target);

    /**
     * Mức xem nhóm theo vai trò, không xét phạm vi hiện hành của đối tượng: cho dòng chi tiết báo cáo của người cao tuổi
     * đã ngoài phạm vi (016 FR-012, Q-196; "Đổi api" 2026-10-24). Người thân xem nhóm có điều kiện bản đồng ý thì không
     * qua hàm này. Mặc định như đối tượng không xác định (chặt hơn), để bản giả của test cũ không phải cài.
     */
    default Visibility canViewByRole(CurrentUser user, InfoGroup group) {
        return canView(user, group, Target.UNSPECIFIED);
    }
}
