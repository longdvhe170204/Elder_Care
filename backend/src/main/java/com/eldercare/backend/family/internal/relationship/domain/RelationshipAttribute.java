package com.eldercare.backend.family.internal.relationship.domain;

import com.eldercare.backend.common.notification.FamilyRight;

/**
 * Thuộc tính có lịch sử của quan hệ ({@code lich_su_quyen.quyen}: sáu quyền, {@code DAI_DIEN}, {@code LIEN_HE_CHINH};
 * data-model mục 2, FR-068). Ghi ở {@code status_history} loại {@link #ENTITY_TYPE}, mỗi lần đổi một dòng {@code POINT}.
 */
public enum RelationshipAttribute {
    VIEW_HEALTH,
    VIEW_COSTS,
    REGISTER_VISITS,
    RECEIVE_URGENT,
    PICKUP_ALLOWED,
    REQUEST_SERVICES,
    REPRESENTATIVE,
    PRIMARY_CONTACT;

    public static final String ENTITY_TYPE = "M10.FAMILY_RELATIONSHIP";

    public static RelationshipAttribute of(FamilyRight right) {
        return valueOf(right.name());
    }
}
