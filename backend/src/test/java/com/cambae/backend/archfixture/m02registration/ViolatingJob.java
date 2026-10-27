package com.cambae.backend.archfixture.m02registration;

import java.util.List;

/** Lớp mẫu: job qua lớp chạy trung gian, lập khai báo trong lambda; luật phải bắt. */
public class ViolatingJob {

    private final Runner runner;

    public ViolatingJob(Runner runner) {
        this.runner = runner;
    }

    public void run() {
        runner.run();
    }

    public static class Runner {

        private final RegistrationService service;

        public Runner(RegistrationService service) {
            this.service = service;
        }

        void run() {
            List.of(1L).forEach(id -> service.create(id));
        }
    }
}
