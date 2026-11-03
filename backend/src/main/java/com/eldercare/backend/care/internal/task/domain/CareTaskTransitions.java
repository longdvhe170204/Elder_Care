package com.eldercare.backend.care.internal.task.domain;

import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.api.dto.TaskStatus;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** FR-026: bảng chuyển thuần; service kiểm quyền, lý do và điều kiện của từng lệnh. */
public final class CareTaskTransitions {
    private CareTaskTransitions() {}

    private static final Map<String, Map<TaskStatus, TaskStatus>> TRANSITIONS = Map.of(
            "WINDOW_OPENED", Map.of(TaskStatus.NOT_YET_DUE, TaskStatus.DUE),
            "WINDOW_EXPIRED", Map.of(TaskStatus.DUE, TaskStatus.OVERDUE),
            "CANCEL", Map.of(TaskStatus.NOT_YET_DUE, TaskStatus.CANCELLED),
            "SOURCE_CANCELLED", Map.of(TaskStatus.NOT_YET_DUE, TaskStatus.CANCELLED),
            "COMPLETE_IN_WINDOW",
                    Map.of(TaskStatus.DUE, TaskStatus.COMPLETED, TaskStatus.OVERDUE, TaskStatus.COMPLETED),
            "COMPLETE_LATE", Map.of(TaskStatus.OVERDUE, TaskStatus.COMPLETED_LATE),
            "DO_NOT_PERFORM",
                    Map.of(TaskStatus.DUE, TaskStatus.NOT_PERFORMED, TaskStatus.OVERDUE, TaskStatus.NOT_PERFORMED),
            "HANDOVER_CONFIRMED", Map.of(TaskStatus.OVERDUE, TaskStatus.NOT_PERFORMED),
            "RESIDENT_FINAL",
                    Map.of(
                            TaskStatus.NOT_YET_DUE,
                            TaskStatus.CANCELLED,
                            TaskStatus.DUE,
                            TaskStatus.NOT_PERFORMED,
                            TaskStatus.OVERDUE,
                            TaskStatus.NOT_PERFORMED));

    public static Optional<TaskStatus> next(TaskStatus status, String event, Importance importance) {
        if (event.equals("HANDOVER_CONFIRMED") && importance != Importance.NORMAL) return Optional.empty();
        return Optional.ofNullable(TRANSITIONS.getOrDefault(event, Map.of()).get(status));
    }

    /** BR-M04-26: caller truyền thời gian nghiệp vụ; đúng hai biên vẫn nằm trong khung. */
    public static TaskStatus initialStatus(Instant windowFrom, Instant windowTo, Instant now) {
        if (windowFrom.isAfter(windowTo)) throw new IllegalArgumentException("Đầu khung phải không sau cuối khung");
        if (now.isBefore(windowFrom)) return TaskStatus.NOT_YET_DUE;
        return now.isAfter(windowTo) ? TaskStatus.OVERDUE : TaskStatus.DUE;
    }
}
