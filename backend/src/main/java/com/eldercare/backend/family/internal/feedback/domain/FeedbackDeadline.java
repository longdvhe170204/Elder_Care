package com.eldercare.backend.family.internal.feedback.domain;

import com.eldercare.backend.family.internal.feedback.domain.Feedback.AssigneeRole;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.ContentGroup;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Priority;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Status;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Type;
import java.time.Duration;
import java.time.Instant;

/**
 * Hạn xử lý phản hồi ({@code HanPhanHoi}; FR-059, FR-061 → FR-064, BR-M10-05; research F-13), hàm thuần. Thời lượng đọc
 * từ CFG-M10-01 lúc tính.
 */
public final class FeedbackDeadline {

    /** CFG-M10-01: hạn theo mức. */
    public record Durations(Duration high, Duration normal) {}

    private FeedbackDeadline() {}

    /** FR-062: Khiếu nại Cao; Góp ý, Hỏi đáp Thường. */
    public static Priority defaultPriority(Type type) {
        return type == Type.COMPLAINT ? Priority.HIGH : Priority.NORMAL;
    }

    /** FR-061: mốc tính hạn (gửi hoặc Mở lại gần nhất) cộng hạn của mức. */
    public static Instant dueAt(Instant basis, Priority priority, Durations durations) {
        return basis.plus(priority == Priority.HIGH ? durations.high() : durations.normal());
    }

    /** FR-059: chăm sóc, sinh hoạt, ăn uống, sức khỏe, thuốc → Trưởng tầng; chi phí, hợp đồng, khác → Hành chính. */
    public static AssigneeRole defaultAssignee(ContentGroup group) {
        return switch (group) {
            case CARE, EATING, HEALTH_MEDICATION -> AssigneeRole.FLOOR_MANAGER;
            case COSTS_CONTRACT, OTHER -> AssigneeRole.OFFICE_STAFF;
        };
    }

    /** FR-063: chưa Đã phản hồi, đã tới hạn, chưa leo thang cho đúng hạn này. */
    public static boolean shouldEscalate(Status status, Instant dueAt, Instant escalatedDueAt, Instant now) {
        boolean open = status == Status.NEW || status == Status.IN_PROGRESS || status == Status.REOPENED;
        return open && !now.isBefore(dueAt) && !dueAt.equals(escalatedDueAt);
    }

    /** FR-064: Đã phản hồi quá CFG-M10-02. */
    public static boolean shouldAutoClose(Status status, Instant repliedAt, Duration autoClose, Instant now) {
        return status == Status.REPLIED && !now.isBefore(repliedAt.plus(autoClose));
    }
}
