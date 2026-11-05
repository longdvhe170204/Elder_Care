package com.eldercare.backend.family.internal.visit.service;

import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import java.time.Instant;
import java.util.function.ToIntFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Việc theo phút của lượt thăm (research F-08; FR-034): Không đến khi hết khung, nhắc ghi giờ ra. */
@Configuration
class VisitTasks {

    @Bean
    M10TimedTask visitNoShow(VisitAutomation automation) {
        return task("VISIT_NO_SHOW", automation::markNoShows);
    }

    @Bean
    M10TimedTask visitCheckOutReminder(VisitAutomation automation) {
        return task("VISIT_CHECK_OUT_REMINDER", automation::remindCheckOut);
    }

    private static M10TimedTask task(String name, ToIntFunction<Instant> run) {
        return new M10TimedTask() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Cadence cadence() {
                return Cadence.MINUTE;
            }

            @Override
            public int run(Instant scheduledAt, Instant actualAt) {
                return run.applyAsInt(actualAt);
            }
        };
    }
}
