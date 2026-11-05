package com.eldercare.backend.family.internal.visit.service;

import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import java.time.Instant;
import java.util.function.ToIntFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Việc theo phút của lượt thăm (research F-08; FR-034): Không đến khi hết khung, nhắc ghi giờ ra. Việc nửa đêm nhắc người
 * cao tuổi có tài khoản về lượt thăm trong ngày (FR-069, US9).
 */
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

    @Bean
    M10TimedTask visitResidentReminder(ResidentVisitReminders reminders) {
        return new M10TimedTask() {
            @Override
            public String name() {
                return "VISIT_RESIDENT_REMINDER";
            }

            @Override
            public Cadence cadence() {
                return Cadence.MIDNIGHT;
            }

            @Override
            public int run(Instant scheduledAt, Instant actualAt) {
                return reminders.remindDay(scheduledAt, actualAt);
            }
        };
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
