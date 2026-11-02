package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.billing.api.events.*;
import com.eldercare.backend.support.fake.billing.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;

/** T002: fixture nguồn do chủ sự kiện E cung cấp; không nghiệm thu producer thật C/D/012. */
class CostSourceFixtureTest {
    static class Recorded {
        final List<Object> events = new ArrayList<>();

        @EventListener
        void billed(CostSourceBilled event) {
            events.add(event);
        }

        @EventListener
        void changed(CostSourceChanged event) {
            events.add(event);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"FAMILY_MEAL", "OVERNIGHT_STAY", "ACTIVITY_ATTENDANCE"})
    void FR_005_oneSharedFixturePublishesBothKindsOfBillingSourceEvents(String type) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(FakeM11Config.class);
            context.registerBean(Recorded.class);
            context.refresh();
            var fixture = context.getBean(FakeCostSources.class);
            var billed = new CostSourceBilled(
                    type,
                    "1",
                    "UNIT",
                    2,
                    Optional.of(3L),
                    BigDecimal.ONE,
                    LocalDate.of(2026, 10, 30),
                    Instant.parse("2026-10-30T01:00:00Z"),
                    "source:1");
            var changed = new CostSourceChanged(
                    type, "1", "UNIT", Optional.empty(), true, "Hủy nguồn kiểm thử", "source:1:cancel");
            fixture.billed(billed);
            fixture.changed(changed);
            assertThat(context.getBean(Recorded.class).events).containsExactly(billed, changed);
        }
    }
}
