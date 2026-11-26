package com.eldercare.backend.care.internal.result.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Ghi nhận lại kết quả (Q-446, BR-M04-14): người đã ghi kết quả của công việc Hoàn thành, Hoàn thành trễ ghi lại bằng
 * bản đính chính cách A khi ca của công việc còn diễn ra; hết ca thì người ghi không tự đính chính nữa.
 */
@Component
public class CareResultReRecording {
    private final ShiftStaffPort shifts;
    private final CareResultStore results;
    private final BusinessTime time;
    private final CurrentUser user;

    public CareResultReRecording(ShiftStaffPort shifts, CareResultStore results, BusinessTime time, CurrentUser user) {
        this.shifts = shifts;
        this.results = results;
        this.time = time;
        this.user = user;
    }

    /** Lệnh "reRecord" của checklist: người dùng là người đã ghi và ca còn diễn ra; kết quả đo lấy từ chỉ số (008). */
    public boolean allowed(CareTask task) {
        if (!Set.of(TaskStatus.COMPLETED, TaskStatus.COMPLETED_LATE).contains(task.status())) return false;
        if ("MEASURED_VALUE".equals(task.data().resultType())) return false;
        return results.find(task.id())
                        .filter(r -> r.recordedBy() == user.accountId())
                        .isPresent()
                && shiftOngoing(task);
    }

    /** Ca của công việc chưa kết thúc tại lúc này. */
    public boolean shiftOngoing(CareTask task) {
        var now = time.now();
        return shifts.shift(task.shiftId())
                .filter(s -> now.isBefore(s.endsAt()))
                .isPresent();
    }
}
