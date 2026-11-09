package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

/** T024/T044: transaction riêng từng dòng hoặc nhóm nhắc gộp; tiếp tục các nhóm khác khi một nhóm lỗi. */
@Service
public class CareTaskDueStep implements CareMinuteWork {
    private final CareTaskStore tasks;
    private final CareTaskClockProcessor processor;
    private final CareTaskNotices notices;

    public CareTaskDueStep(CareTaskStore tasks, CareTaskClockProcessor processor, CareTaskNotices notices) {
        this.tasks = tasks;
        this.processor = processor;
        this.notices = notices;
    }

    @Override
    public int run(Instant now) {
        int count = 0;
        RuntimeException failure = null;
        var batches = new LinkedHashMap<List<Object>, List<Long>>();
        for (var task : tasks.waiting(now)) {
            if (task.status() != TaskStatus.OVERDUE && task.windowTo().isBefore(now.minusSeconds(120))) {
                batches.computeIfAbsent(List.of(task.floorAreaId(), notices.performer(task)), key -> new ArrayList<>())
                        .add(task.id());
                continue;
            }
            try {
                count += processor.advance(task.id(), now);
            } catch (RuntimeException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        for (var group : batches.values()) {
            try {
                count += processor.advanceBatch(group, now);
            } catch (RuntimeException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        if (failure != null) throw failure;
        return count;
    }
}
