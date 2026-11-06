package com.eldercare.backend.nutrition.internal.assignment.service;

import com.eldercare.backend.nutrition.internal.schedule.service.M08MinuteTask;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Việc theo phút: bản gán Chờ hiệu lực tới thời điểm hiệu lực chuyển Hiệu lực (bảng trạng thái bản gán, FR-011, FR-017;
 * research M-15). Xét mọi bản đã tới hạn tại {@code now} nên lần chạy bù sau khi bỏ lỡ vẫn chuyển; mỗi bản một
 * transaction, bản đã chuyển thì bỏ qua (chạy lại không đổi gì).
 */
@Component
class PendingAssignmentTask implements M08MinuteTask {

    private final DietAssignmentService assignments;

    PendingAssignmentTask(DietAssignmentService assignments) {
        this.assignments = assignments;
    }

    @Override
    public String key() {
        return "DIET_ASSIGNMENT_PENDING";
    }

    @Override
    public void run(Instant now) {
        for (long id : assignments.duePending(now)) {
            assignments.activateIfDue(id, now);
        }
    }
}
