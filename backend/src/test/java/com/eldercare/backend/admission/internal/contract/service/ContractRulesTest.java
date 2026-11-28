package com.eldercare.backend.admission.internal.contract.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.admission.internal.contract.domain.ContractContent;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Q-462, FR-019: kỳ tính phí lưu trú phải hợp loại lưu trú khi lập, sửa hợp đồng. */
class ContractRulesTest {

    private static ContractContent content(ResidenceType type, BillingPeriod period) {
        LocalDate start = LocalDate.of(2026, 12, 1);
        return new ContractContent(
                type,
                start,
                type == ResidenceType.SHORT_TERM_RESIDENTIAL ? start.plusDays(20) : null,
                CareLevel.BASIC,
                null,
                null,
                period,
                List.of(),
                true,
                null,
                "Giữ giường theo bảng cơ sở",
                List.of(),
                "Báo trước 30 ngày");
    }

    @ParameterizedTest
    @CsvSource({
        "LONG_TERM_RESIDENTIAL, MONTH",
        "SHORT_TERM_RESIDENTIAL, MONTH",
        "SHORT_TERM_RESIDENTIAL, DAY",
        "DAY_CARE, MONTH",
        "DAY_CARE, SESSION"
    })
    void Q_462_allowedPeriod_passes(ResidenceType type, BillingPeriod period) {
        assertThatCode(() -> ContractRules.validate(content(type, period))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
        "LONG_TERM_RESIDENTIAL, DAY",
        "LONG_TERM_RESIDENTIAL, SESSION",
        "SHORT_TERM_RESIDENTIAL, SESSION",
        "DAY_CARE, DAY"
    })
    void Q_462_periodNotAllowedForType_rejected422(ResidenceType type, BillingPeriod period) {
        assertThatThrownBy(() -> ContractRules.validate(content(type, period)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("BILLING_PERIOD_NOT_ALLOWED");
                    assertThat(e.status().value()).isEqualTo(422);
                });
    }
}
