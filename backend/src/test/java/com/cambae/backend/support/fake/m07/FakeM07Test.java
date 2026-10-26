package com.cambae.backend.support.fake.m07;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m07.api.dto.TimeInterval;
import com.cambae.backend.m07.api.spi.ProcurementMedicationLineView;
import com.cambae.backend.m07.api.spi.PurchaseAssistanceView;
import com.cambae.backend.m07.api.spi.ShiftView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử đơn vị cho bộ bản giả Fake SPI của Module M07 (T002).
 */
class FakeM07Test {

    @Test
    void fakeShiftNursePort_returnsConfiguredData() {
        FakeShiftNursePort fake = new FakeShiftNursePort();
        Instant now = Instant.parse("2026-10-22T08:00:00Z");
        ShiftView shift = new ShiftView(101L, 1L, now, now.plusSeconds(28800), 9001L);

        fake.shift(1L, shift)
                .assignNurse(201L, 9002L)
                .nursesOnShift(1L, List.of(9002L, 9003L))
                .activeStaff(9002L)
                .confirmHandover(101L);

        assertThat(fake.shiftContaining(1L, now)).contains(shift);
        assertThat(fake.assignedNurse(201L, now)).contains(9002L);
        assertThat(fake.nursesOnShift(1L, now)).containsExactly(9002L, 9003L);
        assertThat(fake.isStaffOnActiveShift(9002L, now)).isTrue();
        assertThat(fake.isStaffOnActiveShift(9999L, now)).isFalse();
        assertThat(fake.isHandoverConfirmed(101L)).isTrue();
        assertThat(fake.isHandoverConfirmed(999L)).isFalse();
    }

    @Test
    void fakeTripMedicationPort_returnsConfiguredData() {
        FakeTripMedicationPort fake = new FakeTripMedicationPort();
        Instant from = Instant.parse("2026-10-23T07:00:00Z");
        Instant to = Instant.parse("2026-10-23T17:00:00Z");
        TimeInterval interval = new TimeInterval(from, to);

        fake.approvedTrip(201L, 501L, interval).trip(501L, interval);

        assertThat(fake.approvedTripInterval(201L, 501L)).contains(interval);
        assertThat(fake.tripInterval(501L)).contains(interval);
        assertThat(fake.tripInterval(999L)).isEmpty();
    }

    @Test
    void fakePurchaseAssistancePort_returnsConfiguredData() {
        FakePurchaseAssistancePort fake = new FakePurchaseAssistancePort();
        PurchaseAssistanceView purchase =
                new PurchaseAssistanceView(301L, 201L, "Paracetamol 500mg", BigDecimal.valueOf(10), 9005L);

        fake.recordPurchase(purchase);

        assertThat(fake.purchasedDetails(301L, 201L)).contains(purchase);
        assertThat(fake.purchasedDetails(301L, 999L)).isEmpty();
    }

    @Test
    void fakeProcurementMedicationPort_returnsConfiguredData() {
        FakeProcurementMedicationPort fake = new FakeProcurementMedicationPort();
        ProcurementMedicationLineView line = new ProcurementMedicationLineView(
                401L,
                601L,
                201L,
                701L,
                "Amlodipin 5mg",
                "Amlodipine",
                "5mg",
                "Viên",
                BigDecimal.valueOf(30),
                "viên",
                9001L,
                10L);

        fake.recordLine(line);

        assertThat(fake.lineRecordedPurchased(401L)).contains(line);
        assertThat(fake.lineRecordedPurchased(999L)).isEmpty();
    }
}
