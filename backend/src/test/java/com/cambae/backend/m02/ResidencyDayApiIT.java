package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.ResidencyDayApi;
import com.cambae.backend.m02.api.ResidencyStatisticsApi;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.dto.ResidenceTypeAtDateView;
import com.cambae.backend.m02.api.dto.ResidencyDayView;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T069 (contracts mục 1.4, 1.5a; FR-052, FR-066, FR-070, FR-074, FR-075a): {@code ResidencyDayApi} (tên logic {@code
 * NgayLuuTruApi}) trả mỗi ngày nội trú của hợp đồng đã ký với dữ liệu ngày vắng, các dấu và mốc dừng tính phí;
 * {@code ResidencyStatisticsApi} (tên logic {@code LuuTruThongKeApi}) trả loại lưu trú tại ngày cho 016.
 */
@AutoConfigureMockMvc
@WithMockUser
class ResidencyDayApiIT extends DeathTestBase {

    @Autowired
    ResidencyDayApi days;

    @Autowired
    ResidencyStatisticsApi statistics;

    private ResidencyDayView on(List<ResidencyDayView> list, LocalDate date) {
        return list.stream().filter(d -> d.date().equals(date)).findFirst().orElseThrow();
    }

    @Test
    void FR_052_everyResidentialDayOfTheSignedContract_isReturned_withTheAbsenceDaysMarked() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        long absenceId = grant(residentId, "HOME_VISIT", at(d0.plusDays(4), 17, 0));
        runJobOn(d0.plusDays(2));

        List<ResidencyDayView> list = days.daysBetween(residentId, d0.minusDays(1), d0.plusDays(3));

        assertThat(list)
                .as("ngày trước ngày bắt đầu hợp đồng không có")
                .extracting(ResidencyDayView::date)
                .first()
                .isEqualTo(d0);
        assertThat(list).hasSize(4);
        assertThat(on(list, d0).presence()).isEqualTo("ABSENT");
        assertThat(on(list, d0).absenceId()).isEqualTo(absenceId);
        assertThat(on(list, d0).sequenceNo()).isEqualTo(1);
        assertThat(on(list, d0.plusDays(2)).sequenceNo()).isEqualTo(3);
        assertThat(on(list, d0.plusDays(2)).coefficientPercent()).isNotNull();
        assertThat(on(list, d0.plusDays(3)).presence()).isEqualTo("PRESENT");
        assertThat(on(list, d0.plusDays(3)).absenceId()).isNull();
        assertThat(list).allSatisfy(d -> {
            assertThat(d.contractId()).isNotNull();
            assertThat(d.outsideBillingPeriod()).isFalse();
            assertThat(d.contractOverdue()).isFalse();
        });
    }

    @Test
    void FR_066_aPreparingEnding_stopsBillingAfterTheExpectedEndDate() throws Exception {
        long residentId = inResidence();
        assertThat(days.billingStopDate(residentId)).isEmpty();
        createEnding(residentId, d0.plusDays(2));

        assertThat(days.billingStopDate(residentId)).contains(d0.plusDays(2));
        List<ResidencyDayView> list = days.daysBetween(residentId, d0, d0.plusDays(3));
        assertThat(on(list, d0.plusDays(2)).outsideBillingPeriod()).isFalse();
        assertThat(on(list, d0.plusDays(3)).outsideBillingPeriod()).isTrue();
    }

    @Test
    void FR_070_deathDay_isBilled_andLaterDaysAreOutsideTheBillingPeriod() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 9, 0);
        allItems(residentId, com.cambae.backend.m02.api.dto.PostDeathItemStatus.NOT_COMPLETED);

        doctorRecordsDeath(residentId, null);

        assertThat(days.billingStopDate(residentId)).contains(d0);
        List<ResidencyDayView> list = days.daysBetween(residentId, d0, d0.plusDays(1));
        assertThat(list).hasSize(2);
        assertThat(on(list, d0).outsideBillingPeriod()).isFalse();
        assertThat(on(list, d0.plusDays(1)).outsideBillingPeriod()).isTrue();
    }

    @Test
    void FR_075a_cancelledAdmission_stopsBillingAtTheCancelDate() throws Exception {
        long residentId = inResidence();
        WaitingListTestBase.admin(
                "UPDATE contract SET status = 'TERMINATED', termination_reason = 'Hủy tiếp nhận', terminated_on = '"
                        + d0.plusDays(1) + "' WHERE resident_id = " + residentId);

        assertThat(days.billingStopDate(residentId)).contains(d0.plusDays(1));
        List<ResidencyDayView> list = days.daysBetween(residentId, d0, d0.plusDays(2));
        assertThat(list).hasSize(3);
        assertThat(on(list, d0.plusDays(1)).outsideBillingPeriod()).isFalse();
        assertThat(on(list, d0.plusDays(2)).outsideBillingPeriod()).isTrue();
    }

    @Test
    void FR_074_daysBeforeTheAdmissionDate_areMarkedNotYetAdmitted() throws Exception {
        long residentId = inResidence();
        WaitingListTestBase.admin("INSERT INTO admission_application (resident_id, application_date,"
                + " desired_residence_type, expected_care_level, status, admitted_on, created_by, created_at) VALUES ("
                + residentId + ", '" + d0 + "', 'LONG_TERM_RESIDENTIAL', 'BASIC', 'ADMITTED', '"
                + d0.plusDays(1) + "', 1, NOW(6))");

        List<ResidencyDayView> list = days.daysBetween(residentId, d0, d0.plusDays(2));

        assertThat(on(list, d0).notYetAdmitted()).isTrue();
        assertThat(on(list, d0.plusDays(1)).notYetAdmitted()).isFalse();
    }

    @Test
    void FR_074_theAdmissionDateRecordedOnTheResident_worksWithoutAnApplication() throws Exception {
        long residentId = inResidence();
        WaitingListTestBase.admin("INSERT INTO resident_residency (resident_id, admitted_on) VALUES (" + residentId
                + ", '" + d0.plusDays(1) + "') ON DUPLICATE KEY UPDATE admitted_on = VALUES(admitted_on)");

        List<ResidencyDayView> list = days.daysBetween(residentId, d0, d0.plusDays(2));

        assertThat(on(list, d0).notYetAdmitted()).isTrue();
        assertThat(on(list, d0.plusDays(1)).notYetAdmitted()).isFalse();
    }

    @Test
    void R016_residenceTypeAtDate_showsTheSignedContract_andRespectsTheScope() throws Exception {
        long residentId = inResidence();

        Optional<ResidenceTypeAtDateView> mine = statistics
                .residenceTypeAtDate(ScopeFilter.FACILITY, d0)
                .filter(v -> v.residentId() == residentId)
                .findFirst();

        assertThat(mine).isPresent();
        assertThat(mine.get().contractStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(mine.get().residenceType()).isEqualTo(ResidenceType.LONG_TERM_RESIDENTIAL);
        ScopeFilter other = new ScopeFilter(false, java.util.Set.of(), java.util.Set.of(), java.util.Set.of(-1L));
        assertThat(statistics.residenceTypeAtDate(other, d0)).isEmpty();
        assertThat(statistics
                        .residenceTypeAtDate(ScopeFilter.FACILITY, d0.minusDays(1))
                        .filter(v -> v.residentId() == residentId))
                .isEmpty();
    }
}
