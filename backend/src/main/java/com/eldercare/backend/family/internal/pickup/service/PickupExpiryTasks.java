package com.eldercare.backend.family.internal.pickup.service;

import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Việc theo phút của quy trình đón (research F-07): bản ghi đón chưa dùng quá CFG-M10-09 chuyển Không dùng (FR-027),
 * ngoại lệ Hiệu lực quá CFG-M10-08 chuyển Hết hạn (FR-024). Không lập bản ghi nghiệp vụ (Q-306).
 */
@Configuration
class PickupExpiryTasks {

    @Bean
    M10TimedTask pickupRecordExpiry(PickupPorts ports) {
        return task("PICKUP_RECORD_EXPIRY", ports::expire);
    }

    @Bean
    M10TimedTask pickupExceptionExpiry(PickupService service) {
        return task("PICKUP_EXCEPTION_EXPIRY", service::expireExceptions);
    }

    private static M10TimedTask task(String name, java.util.function.ToIntFunction<Instant> run) {
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
