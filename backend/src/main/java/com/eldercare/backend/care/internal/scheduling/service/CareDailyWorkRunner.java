package com.eldercare.backend.care.internal.scheduling.service;

import com.eldercare.backend.care.internal.scheduling.persistence.CareMilestoneStore;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** NFR-04: marker và tác động của bước cùng transaction; lỗi thì còn mốc để thử lại. */
@Service
public class CareDailyWorkRunner {
    private final CareMilestoneStore store;

    public CareDailyWorkRunner(CareMilestoneStore store) {
        this.store = store;
    }

    @Transactional
    public int run(CareDailyWork work, LocalDate date, Instant now) {
        return store.claim(work.name(), date) ? work.run(date, now) : 0;
    }
}
