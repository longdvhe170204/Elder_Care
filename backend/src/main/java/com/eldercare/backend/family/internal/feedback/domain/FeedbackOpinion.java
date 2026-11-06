package com.eldercare.backend.family.internal.feedback.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Mục ý kiến gắn vào phản hồi Mong muốn ({@code phan_hoi_y_kien}, nhóm 3; R-15: {@code record_event} loại {@link #KIND}
 * gắn {@code M10.FEEDBACK}, khóa duy nhất ở {@code unique_key}; BR-M10-12, FR-058b). Người gắn, thời điểm ở cột của
 * dòng; mục sở thích của 014 gắn qua sự kiện nên không có người gắn.
 */
public record FeedbackOpinion(ItemType itemType, long itemId) {

    public static final SharedKind<FeedbackOpinion> KIND = SharedKind.of("M10.FEEDBACK_OPINION", FeedbackOpinion.class);

    /** Glossary: MONG_MUON_NCT (012), SO_THICH (014). */
    public enum ItemType {
        VISIT_PICKUP_WISH,
        INTEREST
    }
}
