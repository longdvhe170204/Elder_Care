package com.cambae.backend.archfixture.m02registration;

import com.cambae.backend.archfixture.m02registration.persistence.ReminderStore;
import java.util.List;

/** Lớp mẫu: job qua lớp chạy trung gian chỉ chuyển Hết hạn và ghi khóa nhắc (không lập khai báo), hợp lệ. */
public class CompliantJob {

    private final Runner runner;

    public CompliantJob(Runner runner) {
        this.runner = runner;
    }

    public void run() {
        runner.run();
    }

    public static class Runner {

        private final RegistrationService service;
        private final ReminderStore reminders;

        public Runner(RegistrationService service, ReminderStore reminders) {
            this.service = service;
            this.reminders = reminders;
        }

        void run() {
            List.of(1L).forEach(service::expire);
            List.of(1L).forEach(id -> reminders.insert(id));
        }
    }
}
