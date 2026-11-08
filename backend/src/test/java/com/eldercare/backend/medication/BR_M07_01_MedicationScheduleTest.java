package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.catalog.domain.Medication;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.prescription.domain.FrequencyType;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriberType;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.schedule.service.CalculatedDoseSlot;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import com.eldercare.backend.medication.internal.schedule.service.ScheduleContext;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T018: Unit test for Medication Schedule calculation (FR-018 -> FR-023, BR-M07-01).
 * Tests all three frequency patterns, time window resolution, absence handling, and boundaries.
 */
class BR_M07_01_MedicationScheduleTest {

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private MedicationScheduleCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MedicationScheduleCalculator(M07TestParameters.defaults());
    }

    private Prescription createPrescription(
            Long id,
            FrequencyType frequencyType,
            String timedSlots,
            Integer everyNHours,
            Instant firstSlot,
            String daysOfWeek,
            Instant startedAt,
            LocalDate endedAt) {

        Prescription prescription = new Prescription(
                1L,
                100L,
                new BigDecimal("5.00"),
                "pill",
                "Oral",
                PrescriptionType.PERIODIC,
                frequencyType,
                timedSlots,
                everyNHours,
                firstSlot,
                daysOfWeek,
                null,
                null,
                startedAt,
                endedAt,
                MedicationSource.FACILITY,
                PrescriberType.INTERNAL,
                null,
                null,
                10L,
                null,
                null,
                "Uống sau ăn",
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                null,
                Instant.now(),
                java.util.Set.of(),
                Instant.now());

        try {
            Field idField = Prescription.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(prescription, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return prescription;
    }

    private Medication createMedication(Integer windowBefore, Integer windowAfter) {
        return new Medication(
                "Amlodipine 5mg",
                "5mg",
                "Viên nén",
                "Oral",
                "viên",
                windowBefore,
                windowAfter,
                false,
                java.util.Set.of());
    }

    @Test
    @DisplayName("DAYS_OF_WEEK: mã ngày theo ISO, 1 là thứ hai (quyết định chủ dự án 2026-11-08)")
    void testDaysOfWeekIsoMondayIsOne() {
        LocalDate monday = LocalDate.of(2026, 11, 2);
        Instant startedAt = monday.atStartOfDay(ZONE_VN).toInstant();
        Instant to = monday.plusDays(6).atTime(23, 59, 59).atZone(ZONE_VN).toInstant();
        Instant now = monday.atTime(7, 0).atZone(ZONE_VN).toInstant();

        Prescription p =
                createPrescription(103L, FrequencyType.DAYS_OF_WEEK, "[\"09:00\"]", null, null, "1,3", startedAt, null);

        List<CalculatedDoseSlot> schedule =
                calculator.calculate(p, createMedication(30, 30), startedAt, to, now, ScheduleContext.empty(1L));

        assertThat(schedule)
                .extracting(CalculatedDoseSlot::doseKey)
                .containsExactly("103-202611020900", "103-202611040900");
    }

    @Test
    @DisplayName("TIMED_SLOTS: Calculates slots at 08:00, 12:00, 20:00 every day")
    void testTimedSlotsPattern() {
        LocalDate day1 = LocalDate.of(2026, 10, 25);
        LocalDate day2 = LocalDate.of(2026, 10, 26);
        Instant startedAt = day1.atStartOfDay(ZONE_VN).toInstant();
        Instant from = day1.atStartOfDay(ZONE_VN).toInstant();
        Instant to = day2.atTime(23, 59, 59).atZone(ZONE_VN).toInstant();
        Instant now = day1.atTime(7, 0).atZone(ZONE_VN).toInstant();

        Prescription p = createPrescription(
                101L,
                FrequencyType.TIMED_SLOTS,
                "[\"08:00\", \"12:00\", \"20:00\"]",
                null,
                null,
                null,
                startedAt,
                null);

        Medication med = createMedication(30, 30);
        List<CalculatedDoseSlot> schedule = calculator.calculate(p, med, from, to, now, ScheduleContext.empty(1L));

        // 3 slots per day * 2 days = 6 slots
        assertThat(schedule).hasSize(6);

        CalculatedDoseSlot first = schedule.get(0);
        assertThat(first.doseKey()).isEqualTo("101-202610250800");
        assertThat(first.status()).isEqualTo(DoseStatus.UPCOMING);
        assertThat(first.windowFrom())
                .isEqualTo(day1.atTime(7, 30).atZone(ZONE_VN).toInstant());
        assertThat(first.windowTo())
                .isEqualTo(day1.atTime(8, 30).atZone(ZONE_VN).toInstant());

        CalculatedDoseSlot third = schedule.get(2);
        assertThat(third.doseKey()).isEqualTo("101-202610252000");
    }

    @Test
    @DisplayName("EVERY_N_HOURS: Calculates continuous sequence every 6 hours starting at 08:00")
    void testEveryNHoursPattern() {
        LocalDate day1 = LocalDate.of(2026, 10, 25);
        Instant startedAt = day1.atTime(8, 0).atZone(ZONE_VN).toInstant();
        Instant firstSlot = startedAt;
        Instant from = startedAt;
        Instant to = day1.plusDays(1).atTime(8, 0).atZone(ZONE_VN).toInstant();
        Instant now = day1.atTime(8, 15).atZone(ZONE_VN).toInstant(); // inside first window

        Prescription p =
                createPrescription(202L, FrequencyType.EVERY_N_HOURS, null, 6, firstSlot, null, startedAt, null);

        Medication med = createMedication(null, null); // default 30 mins
        List<CalculatedDoseSlot> schedule = calculator.calculate(p, med, from, to, now, ScheduleContext.empty(1L));

        // 08:00, 14:00, 20:00, 02:00 next day, 08:00 next day -> 5 slots
        assertThat(schedule).hasSize(5);

        CalculatedDoseSlot first = schedule.get(0);
        assertThat(first.doseKey()).isEqualTo("202-202610250800");
        // now is 08:15, inside window [07:30, 08:30] -> DUE
        assertThat(first.status()).isEqualTo(DoseStatus.DUE);

        CalculatedDoseSlot second = schedule.get(1);
        assertThat(second.doseKey()).isEqualTo("202-202610251400");
        assertThat(second.status()).isEqualTo(DoseStatus.UPCOMING);
    }
}
