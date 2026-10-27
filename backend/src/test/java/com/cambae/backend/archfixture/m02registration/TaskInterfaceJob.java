package com.cambae.backend.archfixture.m02registration;

import java.util.List;

/**
 * Lớp mẫu: job gọi lớp chạy, lớp chạy gọi từng việc qua interface (như {@code RegistrationDailyRunner} →
 * {@code RegistrationDailyTask}); một bản cài lập khai báo, luật phải bắt; bản cài kia chỉ chuyển Hết hạn.
 */
public class TaskInterfaceJob {

    private final Runner runner;

    public TaskInterfaceJob(Runner runner) {
        this.runner = runner;
    }

    public void run() {
        runner.run();
    }

    public interface DailyTask {
        int run(long day);
    }

    public static class Runner {

        private final List<DailyTask> tasks;

        public Runner(List<DailyTask> tasks) {
            this.tasks = tasks;
        }

        void run() {
            for (DailyTask task : tasks) {
                task.run(1L);
            }
        }
    }

    public static class CreatingTask implements DailyTask {

        private final RegistrationService service;

        public CreatingTask(RegistrationService service) {
            this.service = service;
        }

        @Override
        public int run(long day) {
            service.create(day);
            return 1;
        }
    }

    public static class ExpiringTask implements DailyTask {

        private final RegistrationService service;

        public ExpiringTask(RegistrationService service) {
            this.service = service;
        }

        @Override
        public int run(long day) {
            service.expire(day);
            return 1;
        }
    }
}
