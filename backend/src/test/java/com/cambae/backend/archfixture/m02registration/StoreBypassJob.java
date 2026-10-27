package com.cambae.backend.archfixture.m02registration;

import com.cambae.backend.archfixture.m02registration.persistence.ResidenceRegistrationStore;
import java.util.List;

/** Lớp mẫu: job qua lớp chạy trung gian ghi thẳng bảng khai báo bằng tham chiếu phương thức; luật phải bắt. */
public class StoreBypassJob {

    private final Runner runner;

    public StoreBypassJob(Runner runner) {
        this.runner = runner;
    }

    public void run() {
        runner.run();
    }

    public static class Runner {

        private final ResidenceRegistrationStore store;

        public Runner(ResidenceRegistrationStore store) {
            this.store = store;
        }

        void run() {
            List.of(1L).forEach(store::insert);
        }
    }
}
