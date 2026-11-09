package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.catalog.domain.Medication;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.prescription.domain.FrequencyType;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriberType;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.schedule.service.CalculatedDoseSlot;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleService;
import com.eldercare.backend.medication.internal.status.service.MedicationStatusParticipant;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * BR-M07-03, FR-021, FR-047, FR-048 (US5, quickstart): luồng vắng mặt đầu cuối trên MySQL thật. Tạm vắng không mang
 * thuốc 09:00 → 15:00 làm liều 12:00 Tạm dừng, liều 08:00 và 20:00 vẫn Chưa đến giờ; trở về sớm lúc 10:00 trả liều 12:00
 * về Chưa đến giờ; trạng thái cuối ngừng đơn.
 */
class BR_M07_03_AbsenceFlowIT extends M07IntegrationTestBase {

    @Autowired
    private MedicationStatusParticipant participant;

    @Autowired
    private MedicationScheduleService schedule;

    @Autowired
    private PrescriptionRepository prescriptions;

    @Autowired
    private MedicationRepository medications;

    @Autowired
    private TransactionTemplate tx;

    private final long residentId = TestUsers.nextId();
    private final LocalDate day = LocalDate.now().plusDays(2);
    private Prescription prescription;

    private Instant at(int hour) {
        return day.atTime(LocalTime.of(hour, 0))
                .atZone(MedicationScheduleCalculator.ZONE_VN)
                .toInstant();
    }

    @BeforeEach
    void prescriptionWithThreeDoses() {
        jdbc.update(
                "INSERT INTO resident_module_state (resident_id, module, data, version, updated_at)"
                        + " VALUES (?, 'M07', JSON_OBJECT(), 0, UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE version = version",
                residentId);
        Medication med = medications.save(new Medication(
                "Metformin " + TestUsers.nextId(), "500mg", "Viên", "Uống", "viên", 30, 30, false, Set.of()));
        Instant now = Instant.now();
        prescription = prescriptions.save(new Prescription(
                residentId,
                med.getId(),
                new BigDecimal("1"),
                "viên",
                "Uống",
                PrescriptionType.PERIODIC,
                FrequencyType.TIMED_SLOTS,
                "[\"08:00\",\"12:00\",\"20:00\"]",
                null,
                null,
                null,
                null,
                null,
                now.minus(10, ChronoUnit.DAYS),
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
                10L,
                now,
                Set.of(),
                now));
    }

    private TransitionResult result(StatusCommand command, ResidentStatus from, Instant when, String details) {
        return new TransitionResult(
                residentId,
                from,
                ResidentStatus.IN_RESIDENCE,
                command,
                1L,
                when,
                JsonMapper.builder().build().readTree(details));
    }

    private List<DoseStatus> statuses() {
        return schedule.getResidentSchedule(residentId, day, day, Instant.now()).stream()
                .sorted(java.util.Comparator.comparing(CalculatedDoseSlot::scheduledTime))
                .map(CalculatedDoseSlot::status)
                .toList();
    }

    @Test
    @DisplayName(
            "BR-M07-03: Tạm vắng không mang thuốc Tạm dừng liều trong khoảng, trở về sớm trả liều, trạng thái cuối ngừng đơn")
    void absenceSuspendsDosesReturnRestoresThemAndFinalStatusStopsThePrescription() {
        assertThat(statuses()).containsExactly(DoseStatus.UPCOMING, DoseStatus.UPCOMING, DoseStatus.UPCOMING);

        tx.executeWithoutResult(s -> participant.applyEffects(result(
                StatusCommand.GRANT_TEMPORARY_ABSENCE,
                ResidentStatus.IN_RESIDENCE,
                at(9),
                "{\"takesMedication\":false,\"expectedReturnAt\":\"" + at(15) + "\"}")));
        assertThat(statuses()).containsExactly(DoseStatus.UPCOMING, DoseStatus.SUSPENDED, DoseStatus.UPCOMING);

        tx.executeWithoutResult(s -> participant.applyEffects(result(
                StatusCommand.RECORD_RETURN,
                ResidentStatus.TEMPORARILY_ABSENT,
                at(10),
                "{\"returnedAt\":\"" + at(10) + "\"}")));
        assertThat(statuses()).containsExactly(DoseStatus.UPCOMING, DoseStatus.UPCOMING, DoseStatus.UPCOMING);

        tx.executeWithoutResult(s -> participant.applyEffects(
                result(StatusCommand.RECORD_DEATH, ResidentStatus.IN_RESIDENCE, at(11), "{}")));
        assertThat(prescriptions.findById(prescription.getId()).orElseThrow().getStatus())
                .isEqualTo(PrescriptionStatus.DISCONTINUED);
    }
}
