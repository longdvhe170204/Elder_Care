package com.eldercare.backend.administration.parameter.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void calendarDuration_readAsPeriod() {
        ParameterValue tenYears = value(ParameterKind.DURATION, "{\"value\":10,\"unit\":\"YEARS\"}");

        assertThat(tenYears.asPeriod()).isEqualTo(Period.ofYears(10));
        assertThatThrownBy(tenYears::asDuration).isInstanceOf(IllegalStateException.class);
        assertThat(value(ParameterKind.DURATION, "{\"value\":7,\"unit\":\"DAYS\"}")
                        .asPeriod())
                .isEqualTo(Period.ofDays(7));
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
    void notSetYet_isReportedClearly() {
        ParameterValue notSet = value(ParameterKind.INTEGER, "null");

        assertThat(notSet.isSet()).isFalse();
        assertThatThrownBy(notSet::asInt)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CFG-M15-05");
    }
}
