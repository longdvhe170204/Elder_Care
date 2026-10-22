package com.cambae.backend.m15.parameter.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.document.FacilityInfo;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.Period;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** FR-018 (000 T032): giá trị tham số bất biến, đọc theo kiểu. */
class ParameterValueTest {

    private final JsonMapper json = JsonMapper.builder().build();

    private ParameterValue value(ParameterKind kind, String raw) {
        return new ParameterValue(ParameterCode.CFG_M15_05, kind, json.readTree(raw));
    }

    @Test
    void duration_inMinutesHoursDays() {
        assertThat(value(ParameterKind.DURATION, "{\"value\":48,\"unit\":\"HOURS\"}")
                        .asDuration())
                .isEqualTo(Duration.ofHours(48));
        assertThat(value(ParameterKind.DURATION, "{\"value\":15,\"unit\":\"MINUTES\"}")
                        .asDuration())
                .isEqualTo(Duration.ofMinutes(15));
        assertThat(value(ParameterKind.DURATION, "{\"value\":90,\"unit\":\"DAYS\"}")
                        .asDuration())
                .isEqualTo(Duration.ofDays(90));
        assertThat(value(ParameterKind.DURATION, "{\"value\":30,\"unit\":\"SECONDS\"}")
                        .asDuration())
                .isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void calendarDuration_readAsPeriod() {
        ParameterValue tenYears = value(ParameterKind.DURATION, "{\"value\":10,\"unit\":\"YEARS\"}");

        assertThat(tenYears.asPeriod()).isEqualTo(Period.ofYears(10));
        assertThatThrownBy(tenYears::asDuration).isInstanceOf(IllegalStateException.class);
        assertThat(value(ParameterKind.DURATION, "{\"value\":7,\"unit\":\"DAYS\"}")
                        .asPeriod())
                .isEqualTo(Period.ofDays(7));
    }

    @Test
    void scalarKinds() {
        assertThat(value(ParameterKind.INTEGER, "3").asInt()).isEqualTo(3);
        assertThat(value(ParameterKind.PERCENTAGE, "2.5").asPercent()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(value(ParameterKind.MONEY, "500000").asMoney()).isEqualTo(500_000L);
        assertThat(value(ParameterKind.TIME_OF_DAY, "\"16:00\"").asLocalTime()).isEqualTo(LocalTime.of(16, 0));
    }

    @Test
    void wrongKind_isRejected() {
        assertThatThrownBy(() -> value(ParameterKind.INTEGER, "3").asDuration())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void table_mapsToType() {
        record Lockout(int maxFailedAttempts, Map<String, Object> lockDuration) {}

        Lockout lockout = value(
                        ParameterKind.TABLE,
                        "{\"maxFailedAttempts\":5,\"lockDuration\":{\"value\":15,\"unit\":\"MINUTES\"}}")
                .asTable(Lockout.class);

        assertThat(lockout.maxFailedAttempts()).isEqualTo(5);
    }

    @Test
    void facilityInfo_readsCfgM11_11_withoutLogo() {
        // Dạng giá trị CFG-M11-11 (007 T104): màn Tham số không có ô logo, khóa thiếu thì null
        FacilityInfo facility = value(
                        ParameterKind.TABLE,
                        "{\"name\":\"Viện Bình An\",\"address\":\"Hà Nội\",\"taxCode\":null,\"phone\":\"024 3999 8888\"}")
                .asTable(FacilityInfo.class);

        assertThat(facility).isEqualTo(new FacilityInfo("Viện Bình An", "Hà Nội", null, "024 3999 8888", null));
    }

    @Test
    void notSetYet_isReportedClearly() {
        ParameterValue notSet = value(ParameterKind.INTEGER, "null");

        assertThat(notSet.isSet()).isFalse();
        assertThatThrownBy(notSet::asInt)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CFG-M15-05");
    }
}
