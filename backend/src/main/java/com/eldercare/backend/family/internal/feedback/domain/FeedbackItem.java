package com.eldercare.backend.family.internal.feedback.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.util.List;

/**
 * Mục của phản hồi ({@code phan_hoi_muc}, nhóm 3; R-15: {@code record_event} loại {@link #KIND} gắn
 * {@code M10.FEEDBACK}; FR-065): ghi nhận không sửa, bổ sung bằng mục mới. Người ghi, thời điểm ở cột của dòng.
 *
 * @param healthInformation nội dung xếp loại sức khỏe (trả lời, hướng xử lý của nhóm "sức khỏe, thuốc"; FR-058)
 */
public record FeedbackItem(Type type, String content, boolean healthInformation, List<Long> fileIds) {

    public static final SharedKind<FeedbackItem> KIND = SharedKind.of("M10.FEEDBACK_ITEM", FeedbackItem.class);

    /** Glossary: NOI_DUNG, TRA_LOI, HUONG_XU_LY, GHI_CHU, GIAO_LAI, DOI_MUC, MO_LAI. */
    public enum Type {
        CONTENT,
        REPLY,
        HANDLING_DIRECTION,
        NOTE,
        REASSIGNMENT,
        PRIORITY_CHANGE,
        REOPEN
    }

    public FeedbackItem {
        fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
    }
}
