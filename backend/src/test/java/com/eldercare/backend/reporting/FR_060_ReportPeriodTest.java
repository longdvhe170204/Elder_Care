package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterKind;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.scope.service.ReportPeriods;
import com.eldercare.backend.reporting.internal.scope.service.ReportPeriods.ReportPeriod;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Khoảng tối đa CFG-M14-01 cho báo cáo và xuất (016 T006): FR-005, FR-060. */
class FR_060_ReportPeriodTest {

    private final ParameterApi parameters = new ParameterApi() {
        @Override
        public ParameterValue read(ParameterCode code) {
            return new ParameterValue(
                    code,
                    ParameterKind.DURATION,
                    JsonMapper.builder().build().readTree("{\"value\":12,\"unit\":\"MONTHS\"}"));
        }

        @Override
        public ParameterValue readAt(ParameterCode code, Instant at) {
            return read(code);
        }
    };

    private final ReportPeriods periods = new ReportPeriods(
            parameters, new BusinessTime(Clock.fixed(Instant.parse("2026-10-24T03:00:00Z"), BusinessTime.ZONE)));

    @Test
    void FR_060_twelveMonthsAccepted_FR_005_inclusiveDaysInVietnamTime() {
        ReportPeriod p = periods.check(LocalDate.of(2025, 11, 1), LocalDate.of(2026, 10, 31));

        assertThat(p.start()).isEqualTo(Instant.parse("2025-10-31T17:00:00Z"));
        assertThat(p.end()).isEqualTo(Instant.parse("2026-10-31T17:00:00Z"));
    }

    @Test
    void FR_060_longerThanMaximum_rejectedWithLimit_notTruncated() {
        assertThatThrownBy(() -> periods.check(LocalDate.of(2025, 11, 1), LocalDate.of(2026, 11, 1)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("RANGE_EXCEEDS_MAXIMUM");
                    assertThat(e.getMessage()).contains("12 tháng");
                });
    }

    @Test
    void toBeforeFrom_invalid() {
        assertThatThrownBy(() -> periods.check(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(InvalidValueException.class);
    }
}
