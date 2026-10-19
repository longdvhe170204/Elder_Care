package com.cambae.backend.common.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Nguồn thời gian duy nhất của backend (research R-04, NFR-09). Profile {@code test}, {@code demo} dùng
 * {@link TestClockControl} thay cho bean này.
 */
@Configuration
public class ClockConfig {

    @Bean
    @Profile("!test & !demo")
    Clock clock() {
        return Clock.system(BusinessTime.ZONE);
    }
}
