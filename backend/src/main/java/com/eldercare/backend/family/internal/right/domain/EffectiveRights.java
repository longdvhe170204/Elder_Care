package com.eldercare.backend.family.internal.right.domain;

import com.eldercare.backend.common.notification.FamilyRight;

/**
 * Quyền có tác dụng ({@code QuyenCoTacDung}; research F-04, FR-011, DBR-03): quyền đang bật; riêng xem sức khỏe còn cần
 * bản đồng ý Hiệu lực của người cao tuổi bao gồm người thân tại thời điểm xem. Bản đồng ý đổi thì quyền giữ giá trị.
 */
public final class EffectiveRights {

    private EffectiveRights() {}

    public static boolean effective(FamilyRight right, boolean enabled, boolean consentIncludesFamilyMember) {
        return enabled && (right != FamilyRight.VIEW_HEALTH || consentIncludesFamilyMember);
    }

    /** Hiển thị "không có tác dụng" cho Hành chính và người thân (FR-011). */
    public static boolean enabledButNotEffective(
            FamilyRight right, boolean enabled, boolean consentIncludesFamilyMember) {
        return enabled && !effective(right, true, consentIncludesFamilyMember);
    }
}
