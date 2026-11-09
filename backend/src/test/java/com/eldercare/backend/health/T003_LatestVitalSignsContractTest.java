package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.health.api.VitalSignApi;
import com.eldercare.backend.health.api.dto.VitalSignHandoverView;
import com.eldercare.backend.support.fake.health.FakeVitalSigns;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class T003_LatestVitalSignsContractTest {

    private static final Instant MEASURED_AT = Instant.parse("2026-10-31T07:00:00Z");

    @Test
    void FR_050_latestValuesRemainEmptyUntilTheSourceIsConfigured() {
        VitalSignApi api = new FakeVitalSigns();

        assertThat(api.latestPerType(101)).isEmpty();
    }

    @Test
    void FR_083_existingHandoverSignatureAndTimeWindowRemainAvailable() {
        var first = new VitalSignHandoverView(201, 101, 401L, "Chỉ số thử", MEASURED_AT);
        var atEnd = new VitalSignHandoverView(202, 102, 401L, "Chỉ số thử ở biên", MEASURED_AT.plusSeconds(60));
        var fake = new FakeVitalSigns();
        fake.source(501, List.of(first, atEnd));
        VitalSignApi api = fake;

        assertThat(api.aboveThresholdInRange(501, MEASURED_AT, MEASURED_AT.plusSeconds(60)))
                .containsExactly(first);
        assertThat(api.latestPerType(101)).isEmpty();
    }

    @Test
    void T003_sharedConfigExposesTheExistingFakeThroughTheExtendedApi() {
        try (var context = new AnnotationConfigApplicationContext(FakeVitalSigns.Config.class)) {
            var api = context.getBean(VitalSignApi.class);

            assertThat(api).isSameAs(context.getBean(FakeVitalSigns.class));
            assertThat(api.latestPerType(101)).isEmpty();
        }
    }
}
