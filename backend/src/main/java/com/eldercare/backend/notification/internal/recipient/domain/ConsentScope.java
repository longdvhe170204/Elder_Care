package com.eldercare.backend.notification.internal.recipient.domain;

import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.NotificationLevel;

/**
 * Xét phạm vi "nhận thông báo" của bản đồng ý cho một người thân ({@code XetPhamViDongY}; FR-001a, FR-014, FR-014a, Q-353,
 * Q-354, research B-04); chạy trước {@link ContentFilter}.
 */
public final class ConsentScope {

    public enum Decision {
        /** Gửi như thường. */
        SEND,
        /** Ngoài phạm vi, có mã Q-354: gửi và ghi mã vào thông báo của người đó. */
        SEND_WITH_CASE,
        /** Ngoài phạm vi, không mã, mức không khẩn: không gửi người đó, không phải lỗi. */
        SKIP,
        /** Ngoài phạm vi, không mã, mức Khẩn cấp: không gửi người đó, ghi lỗi gửi thiếu mã trường hợp. */
        MISSING_CASE
    }

    private ConsentScope() {}

    /**
     * @param subject thiếu thì coi như về người cao tuổi
     * @param inScope người cao tuổi có bản đồng ý Hiệu lực với mục "nhận thông báo" bao gồm người thân này
     */
    public static Decision decide(
            InformationSubject subject, boolean inScope, boolean hasCase, NotificationLevel level) {
        if (subject == InformationSubject.ABOUT_FAMILY_MATTER || inScope) {
            return Decision.SEND;
        }
        if (hasCase) {
            return Decision.SEND_WITH_CASE;
        }
        return level == NotificationLevel.URGENT ? Decision.MISSING_CASE : Decision.SKIP;
    }
}
