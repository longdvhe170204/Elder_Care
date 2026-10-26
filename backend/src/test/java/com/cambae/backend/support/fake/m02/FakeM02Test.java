package com.cambae.backend.support.fake.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m02.api.dto.Attendance;
import com.cambae.backend.m02.api.dto.DepositStatus;
import com.cambae.backend.m02.api.dto.PickupRecordView;
import com.cambae.backend.m02.api.dto.PostDeathItemCode;
import com.cambae.backend.m02.api.dto.PostDeathItemStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** 004 T002: bản giả các SPI M02 khai báo cho module khác cài. */
class FakeM02Test {

    private static final LocalDate D = LocalDate.of(2026, 10, 25);

    @Test
    void DBR_02_representative_defaultsToNoneAndReflectsConfiguredRepresentative() {
        FakeRepresentatives representatives = new FakeRepresentatives();
        assertThat(representatives.hasRepresentative(1)).isFalse();

        representatives.representative(1, 7);

        assertThat(representatives.hasRepresentative(1)).isTrue();
        assertThat(representatives.isActiveRepresentative(7, 1, D)).isTrue();
        assertThat(representatives.isActiveRepresentative(8, 1, D)).isFalse();
    }

    @Test
    void FR_048_pickupRecord_secondUseIsRejected() {
        FakePickupRecords records = new FakePickupRecords();
        records.valid(1, 7, new PickupRecordView(5, 1, "FAMILY"));

        assertThat(records.validPickupRecord(1, 7, Instant.EPOCH)).isPresent();
        assertThat(records.validPickupRecord(1, 8, Instant.EPOCH)).isEmpty();

        records.markUsed(5, "GRANT_TEMPORARY_ABSENCE", 11);

        assertThat(records.isUsed(5)).isTrue();
        assertThatThrownBy(() -> records.markUsed(5, "GRANT_TEMPORARY_ABSENCE", 12))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("PICKUP_RECORD_ALREADY_USED"));
    }

    @Test
    void FR_031_depositStatus_defaultsToNotRequired() {
        FakeDepositStatuses statuses = new FakeDepositStatuses();
        assertThat(statuses.status(1).status()).isEqualTo(DepositStatus.NOT_REQUIRED);

        statuses.status(1, DepositStatus.NOT_SATISFIED);

        assertThat(statuses.status(1).status()).isEqualTo(DepositStatus.NOT_SATISFIED);
    }

    @Test
    void FR_066_finalPeriod_recordsCallsAndCanFailToForceRollback() {
        FakeFinalPeriods periods = new FakeFinalPeriods();

        long id = periods.createDraft(1, D);
        periods.recalculate(1, D.plusDays(3));
        periods.cancelStop(1);

        assertThat(id).isEqualTo(1);
        assertThat(periods.calls())
                .containsExactly("createDraft:1:2026-10-25", "recalculate:1:2026-10-28", "cancelStop:1");

        periods.failOnCall();
        assertThatThrownBy(() -> periods.cancelStop(1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void FR_060_dayCareAttendance_undeterminedSessionIsEmpty() {
        FakeDayCareAttendance attendance = new FakeDayCareAttendance();
        assertThat(attendance.status(1, D, 2)).isEmpty();

        attendance.attendance(1, D, 2, Attendance.ABSENT_NOTIFIED);

        assertThat(attendance.status(1, D, 2)).contains(Attendance.ABSENT_NOTIFIED);
    }

    @Test
    void FR_071_postDeathItem_reportsItsCodeAndDefaultsToNotApplicable() {
        FakePostDeathItem item = new FakePostDeathItem(PostDeathItemCode.STORED_MEDICATION);

        assertThat(item.code()).isEqualTo(PostDeathItemCode.STORED_MEDICATION);
        assertThat(item.status(1).status()).isEqualTo(PostDeathItemStatus.NOT_APPLICABLE);

        item.status(1, PostDeathItemStatus.NOT_COMPLETED);

        assertThat(item.status(1).status()).isEqualTo(PostDeathItemStatus.NOT_COMPLETED);
    }
}
