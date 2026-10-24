package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m07.api.dto.MedicationSource;
import com.cambae.backend.m07.internal.domain.DoseStatus;
import com.cambae.backend.m07.internal.domain.FrequencyType;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.PrescriberType;
import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.domain.PrescriptionStatus;
import com.cambae.backend.m07.internal.domain.PrescriptionType;
import com.cambae.backend.m07.internal.service.AbsenceType;
import com.cambae.backend.m07.internal.service.CalculatedDoseSlot;
import com.cambae.backend.m07.internal.service.MedicationScheduleCalculator;
import com.cambae.backend.m07.internal.service.ResidentAbsenceInterval;
import com.cambae.backend.m07.internal.service.ScheduleContext;
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
        calculator = new MedicationScheduleCalculator();
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
                java.util.Set.of());

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

    @Test
    @DisplayName("DAYS_OF_WEEK: Only schedules on configured days of week")
    void testDaysOfWeekPattern() {
        // 2026-10-25 is Sunday (1 in DB SET)
        // 2026-10-26 is Monday (2 in DB SET)
        // 2026-10-27 is Tuesday (3 in DB SET)
        // 2026-10-28 is Wednesday (4 in DB SET)
        LocalDate day1 = LocalDate.of(2026, 10, 25); // Sunday
        LocalDate day4 = LocalDate.of(2026, 10, 28); // Wednesday
        Instant from = day1.atStartOfDay(ZONE_VN).toInstant();
        Instant to = day4.atTime(23, 59, 59).atZone(ZONE_VN).toInstant();
        Instant now = from;

        // Schedule only on Sunday (1) and Tuesday (3) at 09:00
        Prescription p =
                createPrescription(303L, FrequencyType.DAYS_OF_WEEK, "[\"09:00\"]", null, null, "1,3", from, null);

        Medication med = createMedication(15, 45); // custom window
        List<CalculatedDoseSlot> schedule = calculator.calculate(p, med, from, to, now, ScheduleContext.empty(1L));

        // Exactly 2 slots: Sunday 2026-10-25 and Tuesday 2026-10-27
        assertThat(schedule).hasSize(2);
        assertThat(schedule.get(0).doseKey()).isEqualTo("303-202610250900");
        assertThat(schedule.get(0).windowFrom())
                .isEqualTo(day1.atTime(8, 45).atZone(ZONE_VN).toInstant());
        assertThat(schedule.get(0).windowTo())
                .isEqualTo(day1.atTime(9, 45).atZone(ZONE_VN).toInstant());
        assertThat(schedule.get(1).doseKey()).isEqualTo("303-202610270900");
    }

    @Test
    @DisplayName("Absence intervals: Doses during leave are SUSPENDED or LEAVE_MEDICATION")
    void testAbsenceIntervalHandling() {
        LocalDate day = LocalDate.of(2026, 10, 25);
        Instant startedAt = day.atStartOfDay(ZONE_VN).toInstant();
        Instant from = startedAt;
        Instant to = day.atTime(23, 59, 59).atZone(ZONE_VN).toInstant();
        Instant now = day.atTime(6, 0).atZone(ZONE_VN).toInstant();

        // Prescription with doses at 08:00, 12:00, 20:00
        Prescription p = createPrescription(
                404L,
                FrequencyType.TIMED_SLOTS,
                "[\"08:00\", \"12:00\", \"20:00\"]",
                null,
                null,
                null,
                startedAt,
                null);

        // Resident has temporary leave without medication between 10:00 and 15:00
        ScheduleContext context = new ScheduleContext(
                1L,
                List.of(new ResidentAbsenceInterval(
                        1L,
                        day.atTime(10, 0).atZone(ZONE_VN).toInstant(),
                        day.atTime(15, 0).atZone(ZONE_VN).toInstant(),
                        AbsenceType.TEMPORARY_LEAVE)),
                null);

        Medication med = createMedication(30, 30);
        List<CalculatedDoseSlot> schedule = calculator.calculate(p, med, from, to, now, context);

        assertThat(schedule).hasSize(3);
        assertThat(schedule.get(0).status()).isEqualTo(DoseStatus.UPCOMING); // 08:00
        assertThat(schedule.get(1).status()).isEqualTo(DoseStatus.SUSPENDED); // 12:00 (inside absence)
        assertThat(schedule.get(2).status()).isEqualTo(DoseStatus.UPCOMING); // 20:00
    }

    @Test
    @DisplayName("PRN prescription: Produces no pre-scheduled slots")
    void testPrnProducesNoSlots() {
        Prescription p = new Prescription(
                1L,
                100L,
                new BigDecimal("500"),
                "mg",
                "Oral",
                PrescriptionType.PRN,
                null,
                null,
                null,
                null,
                null,
                240,
                4,
                Instant.now(),
                null,
                MedicationSource.FACILITY,
                PrescriberType.INTERNAL,
                null,
                null,
                10L,
                null,
                null,
                null,
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                null,
                Instant.now(),
                java.util.Set.of());

        List<CalculatedDoseSlot> schedule = calculator.calculate(
                p, null, Instant.now(), Instant.now().plusSeconds(86400), Instant.now(), ScheduleContext.empty(1L));
        assertThat(schedule).isEmpty();
    }
}
