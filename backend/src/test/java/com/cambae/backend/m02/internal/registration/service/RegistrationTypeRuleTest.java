package com.cambae.backend.m02.internal.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m02.internal.registration.domain.RegistrationType;
import com.cambae.backend.m02.internal.registration.service.ExpectedStayDuration.StayPeriod;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 018 T003 (FR-001, FR-007; BR-M02-11, BR-M02-14, Q-215; research Z-03): Đăng ký tạm trú khi tổng thời gian lưu trú dự
 * kiến từ CFG-M02-11 trở lên hoặc không có ngày kết thúc; ngược lại Thông báo lưu trú; người được xác nhận thường trú cùng
 * xã/phường luôn Thông báo lưu trú.
 */
class RegistrationTypeRuleTest {

    private static final int THRESHOLD = 30;

    private static ExpectedStayDuration stay(String start, String end) {
        return ExpectedStayDuration.of(
                List.of(new StayPeriod(LocalDate.parse(start), end == null ? null : LocalDate.parse(end))), null);
    }

    @Test
    void justBelowTheThreshold_isStayNotification() {
        assertThat(RegistrationTypeRule.determine(stay("2026-10-01", "2026-10-29"), THRESHOLD, false))
                .isEqualTo(RegistrationType.STAY_NOTIFICATION);
    }

    @Test
    void exactlyTheThreshold_isTemporaryResidenceRegistration() {
        assertThat(RegistrationTypeRule.determine(stay("2026-10-01", "2026-10-30"), THRESHOLD, false))
                .isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
    }

    @Test
    void aboveTheThreshold_isTemporaryResidenceRegistration() {
        assertThat(RegistrationTypeRule.determine(stay("2026-10-01", "2026-10-31"), THRESHOLD, false))
                .isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
    }

    @Test
    void noEndDate_isTemporaryResidenceRegistration() {
        assertThat(RegistrationTypeRule.determine(stay("2026-10-01", null), THRESHOLD, false))
                .isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
    }

    /** Quickstart Q3 (B 14 ngày), Q16 (E 21 ngày sau gia hạn), Q14 (B lên 60 ngày). */
    @Test
    void quickstartCases() {
        assertThat(RegistrationTypeRule.determine(stay("2026-10-01", "2026-10-14"), THRESHOLD, false))
                .isEqualTo(RegistrationType.STAY_NOTIFICATION);
        assertThat(RegistrationTypeRule.determine(stay("2026-10-01", "2026-10-21"), THRESHOLD, false))
                .isEqualTo(RegistrationType.STAY_NOTIFICATION);
        assertThat(RegistrationTypeRule.determine(stay("2026-10-01", "2026-11-29"), THRESHOLD, false))
                .isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
    }

    @Test
    void localPermanentResident_isAlwaysStayNotification() {
        assertThat(RegistrationTypeRule.determine(stay("2026-10-01", "2027-09-30"), THRESHOLD, true))
                .isEqualTo(RegistrationType.STAY_NOTIFICATION);
        assertThat(RegistrationTypeRule.determine(stay("2026-10-01", null), THRESHOLD, true))
                .isEqualTo(RegistrationType.STAY_NOTIFICATION);
    }

    @Test
    void changedThreshold_isApplied() {
        assertThat(RegistrationTypeRule.determine(stay("2026-10-01", "2026-10-29"), 29, false))
                .isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
    }

    @Test
    void thresholdBelowOneDay_isRejected() {
        assertThatThrownBy(() -> RegistrationTypeRule.determine(stay("2026-10-01", "2026-10-29"), 0, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
