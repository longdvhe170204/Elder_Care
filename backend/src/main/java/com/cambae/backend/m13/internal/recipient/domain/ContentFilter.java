package com.cambae.backend.m13.internal.recipient.domain;

import com.cambae.backend.common.notification.FamilyRight;
import com.cambae.backend.common.notification.InformationType;
import java.util.EnumSet;
import java.util.Set;

/**
 * Phần nội dung người thân được thấy ({@code LocPhanNoiDung}; FR-013, DBR-03, Q-112, research B-04). "Chung" luôn; sức khỏe
 * cần quyền xem sức khỏe và bản đồng ý Hiệu lực bao gồm người đó; chi phí, thăm nom theo quyền; hợp đồng chỉ người đại
 * diện. Người thân không có quan hệ với người cao tuổi (nhóm (d)) chỉ phần chung.
 */
public final class ContentFilter {

    private ContentFilter() {}

    /** Chỉ trả loại có trong {@code parts} của yêu cầu; rỗng thì người đó không nhận. */
    public static Set<InformationType> visibleParts(
            Set<InformationType> parts,
            Set<FamilyRight> rights,
            boolean representative,
            boolean related,
            boolean consentIncludes) {
        Set<InformationType> allowed = EnumSet.of(InformationType.GENERAL);
        if (related) {
            if (rights.contains(FamilyRight.VIEW_HEALTH) && consentIncludes) {
                allowed.add(InformationType.HEALTH);
            }
            if (rights.contains(FamilyRight.VIEW_COSTS)) {
                allowed.add(InformationType.COSTS);
            }
            if (rights.contains(FamilyRight.REGISTER_VISITS)) {
                allowed.add(InformationType.VISITS);
            }
            if (representative) {
                allowed.add(InformationType.CONTRACT);
            }
        }
        allowed.retainAll(parts);
        return allowed;
    }
}
