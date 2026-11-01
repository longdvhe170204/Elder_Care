package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.domain.CareTaskTransitions;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T024: chỉ chuyển trạng thái đã tới mốc, kể cả chạy bù; không có lệnh lập công việc. */
@Service
public class CareTaskDueStep implements CareMinuteWork {
    private final CareTaskStore tasks;
    private final CareLockStore locks;
    private final CareTaskService commands;
    private final CareTaskNotices notices;
    private final ResidentApi residents;

    public CareTaskDueStep(
            CareTaskStore tasks,
            CareLockStore locks,
            CareTaskService commands,
            CareTaskNotices notices,
            ResidentApi residents) {
        this.tasks = tasks;
        this.locks = locks;
        this.commands = commands;
        this.notices = notices;
        this.residents = residents;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int run(Instant now) {
        var candidates = tasks.waiting(now);
        locks.lock(candidates.stream()
                .map(t -> t.residentId())
                .filter(Objects::nonNull)
                .toList());
        int count = 0;
        for (var candidate : candidates) {
            var before = tasks.get(candidate.id(), true);
            if (before.status() != TaskStatus.NOT_YET_DUE
                    || (before.residentId() != null && residents.isFinalStatus(before.residentId()))) continue;
            var next = CareTaskTransitions.initialStatus(before.windowFrom(), before.windowTo(), now);
            if (next == TaskStatus.NOT_YET_DUE) continue;
            var after = tasks.transition(before, next, now, null, null);
            commands.record(before, after, "TIME_CHANGED", null, true);
            if (next == TaskStatus.OVERDUE) notices.overdue(after, now);
            count++;
        }
        return count;
    }
}
