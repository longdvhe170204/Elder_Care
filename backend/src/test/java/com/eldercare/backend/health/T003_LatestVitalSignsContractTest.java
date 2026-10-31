package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.health.api.VitalSignApi;
import com.eldercare.backend.health.api.dto.LatestVitalSignView;
import com.eldercare.backend.health.api.dto.VitalSignHandoverView;
import com.eldercare.backend.support.fake.health.FakeVitalSigns;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    void FR_050_latestValuesPreserveComponentsMetadataAndMeasurementTime() {
        var components =
                new HashMap<>(Map.of("SYSTOLIC", new BigDecimal("120.00"), "DIASTOLIC", new BigDecimal("80.00")));
        var reading = new LatestVitalSignView(201, 301, "BLOOD_PRESSURE", "Huyết áp", "mmHg", components, MEASURED_AT);
        var configured = new HashMap<>(Map.of(301L, reading));
        var fake = new FakeVitalSigns();
        fake.latest(101, configured);
        components.clear();
        configured.clear();
        VitalSignApi api = fake;

        assertThat(api.latestPerType(101)).containsOnlyKeys(301L);
        var actual = api.latestPerType(101).get(301L);
        assertThat(actual.recordId()).isEqualTo(201);
        assertThat(actual.indicatorId()).isEqualTo(301);
        assertThat(actual.indicatorCode()).isEqualTo("BLOOD_PRESSURE");
        assertThat(actual.indicatorName()).isEqualTo("Huyết áp");
        assertThat(actual.unit()).isEqualTo("mmHg");
        assertThat(actual.values())
                .containsEntry("SYSTOLIC", new BigDecimal("120.00"))
                .containsEntry("DIASTOLIC", new BigDecimal("80.00"));
        assertThat(actual.measuredAt()).isEqualTo(MEASURED_AT);
        assertThat(api.latestPerType(102)).isEmpty();
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
