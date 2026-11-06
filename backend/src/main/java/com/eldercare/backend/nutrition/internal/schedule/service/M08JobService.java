package com.eldercare.backend.nutrition.internal.schedule.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Chạy các việc định kỳ của M08 (research M-15). Một việc lỗi không chặn việc khác; sau khi chạy hết, lỗi đầu tiên được
 * ném lại để lần chạy ghi kết quả thất bại và lần sau chạy bù. Không chốt suất, không lập phiếu (Q-306).
 */
@Service
public class M08JobService {

    private static final Logger log = LoggerFactory.getLogger(M08JobService.class);

    private final List<M08MinuteTask> minuteTasks;
    private final List<M08DailyTask> dailyTasks;

    public M08JobService(List<M08MinuteTask> minuteTasks, List<M08DailyTask> dailyTasks) {
        this.minuteTasks = minuteTasks;
        this.dailyTasks = dailyTasks;
    }

    public void runMinuteTasks(Instant now) {
        runAll(minuteTasks, M08MinuteTask::key, task -> task.run(now));
    }

    public void runDailyTasks(LocalDate today, Instant now) {
        runAll(dailyTasks, M08DailyTask::key, task -> task.run(today, now));
    }

    private static <T> void runAll(List<T> tasks, java.util.function.Function<T, String> key, Consumer<T> run) {
        RuntimeException first = null;
        for (T task : tasks) {
            try {
                run.accept(task);
            } catch (RuntimeException error) {
                log.error("Việc M08 {} lỗi", key.apply(task), error);
                if (first == null) {
                    first = error;
                } else {
                    first.addSuppressed(error);
                }
            }
        }
        if (first != null) {
            throw first;
        }
    }
}
