package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.domain.DelivererType;
import com.eldercare.backend.medication.internal.domain.DoseAuthorityBasis;
import com.eldercare.backend.medication.internal.domain.DoseResult;
import com.eldercare.backend.medication.internal.domain.DoseResultType;
import com.eldercare.backend.medication.internal.domain.DoseReview;
import com.eldercare.backend.medication.internal.domain.DoseStatus;
import com.eldercare.backend.medication.internal.domain.FamilyMedicationBatch;
import com.eldercare.backend.medication.internal.domain.FamilyMedicationBatchStatus;
import com.eldercare.backend.medication.internal.domain.FamilyMedicationPrescription;
import com.eldercare.backend.medication.internal.domain.FrequencyType;
import com.eldercare.backend.medication.internal.domain.Medication;
import com.eldercare.backend.medication.internal.domain.MedicationDose;
import com.eldercare.backend.medication.internal.domain.PrescriberType;
import com.eldercare.backend.medication.internal.domain.Prescription;
import com.eldercare.backend.medication.internal.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.domain.PrnAdministration;
import com.eldercare.backend.medication.internal.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.persistence.DoseReviewRepository;
import com.eldercare.backend.medication.internal.persistence.FamilyMedicationBatchRepository;
import com.eldercare.backend.medication.internal.persistence.FamilyMedicationPrescriptionRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.persistence.PrnAdministrationRepository;
import com.eldercare.backend.medication.internal.service.MedicationScheduleCalculator;
import com.eldercare.backend.medication.web.DoseController;
import com.eldercare.backend.medication.web.MedicationHandoverController;
import com.eldercare.backend.medication.web.PrnMedicationController;
import com.eldercare.backend.medication.web.response.DoseBodies.DoseReviewBody;
import com.eldercare.backend.medication.web.response.HandoverBodies.MedicationToBringBody;
import com.eldercare.backend.medication.web.response.PrnBodies.PrnAdministrationBody;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Phản hồi theo api-006 (bổ sung khi sửa operation 2026-10-26): thuốc cần mang kèm lô dùng được (FR-048a, FR-044), lần
 * dùng PRN đủ trường đã ghi (FR-024), bản ghi chờ xem lại đủ để nhận ra liều và người ghi (FR-029); người thực hiện bằng
 * họ tên.
 */
class M07ResponseFieldsIT extends M07IntegrationTestBase {

    @Autowired
    MedicationHandoverController handovers;

    @Autowired
    PrnMedicationController prn;

    @Autowired
    DoseController doses;

    @Autowired
    MedicationRepository medicationRepository;

    @Autowired
    PrescriptionRepository prescriptionRepository;

    @Autowired
    FamilyMedicationBatchRepository batchRepository;

    @Autowired
    FamilyMedicationPrescriptionRepository batchLinkRepository;

    @Autowired
    PrnAdministrationRepository prnRepository;

    @Autowired
    MedicationDoseRepository doseRepository;

    @Autowired
    DoseResultRepository doseResultRepository;

    @Autowired
    DoseReviewRepository reviewRepository;

    @Autowired
    FakeStaffProfiles staffProfiles;

    long residentId;
    Medication medication;
    Prescription prescription;
    FamilyMedicationBatch batch;

    @BeforeEach
    void familySentPrescription() {
        residentId = TestUsers.nextId();
        Instant now = time.now();
        medication = medicationRepository.save(new Medication(
                "Amlodipin " + TestUsers.nextId(), "5mg", "Viên nén", "Uống", "viên", 30, 30, false, Set.of(201L)));
        prescription = prescriptionRepository.save(new Prescription(
                residentId,
                medication.getId(),
                new BigDecimal("1"),
                "viên",
                "Uống",
                PrescriptionType.PERIODIC,
                FrequencyType.TIMED_SLOTS,
                "[\"08:00\"]",
                null,
                null,
                null,
                null,
                null,
                now.minus(Duration.ofDays(1)),
                null,
                MedicationSource.FAMILY_SENT,
                PrescriberType.INTERNAL,
                null,
                null,
                doctor.accountId(),
                null,
                null,
                null,
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                doctor.accountId(),
                now.minus(Duration.ofDays(1)),
                Set.of(201L),
                now));
        batch = batchRepository.save(new FamilyMedicationBatch(
                residentId,
                medication.getId(),
                null,
                null,
                "5mg",
                new BigDecimal("30"),
                "viên",
                LocalDate.now().plusYears(1),
                "Nguyên vỉ",
                true,
                null,
                DelivererType.FREE,
                null,
                "Người nhà",
                "Con",
                null,
                nurse.accountId(),
                now.minus(Duration.ofDays(1)),
                FamilyMedicationBatchStatus.IN_USE,
                null,
                now));
        batchLinkRepository.save(
                new FamilyMedicationPrescription(batch.getId(), prescription.getId(), now.minus(Duration.ofDays(1))));
        staffProfiles.name(nurse.staffId(), "Điều dưỡng Lan");
        user.set(nurse.user());
        assignNurse(nurse, residentId);
    }

    @Test
    void FR_048a_medicationsToBring_showSourceAndUsableBatches() {
        MedicationToBringBody item = handovers.getMedicationsToBring(residentId, null).stream()
                .filter(m -> m.prescriptionId().equals(prescription.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(item.medicationSource()).isEqualTo("FAMILY_SENT");
        assertThat(item.usableBatches()).singleElement().satisfies(b -> {
            assertThat(b.batchId()).isEqualTo(batch.getId());
            assertThat(b.expiryDate()).isEqualTo(batch.getExpiryDate());
        });
    }

    @Test
    void FR_024_prnHistory_returnsRecordedFields_andRecorderName() {
        Instant at = time.now().truncatedTo(ChronoUnit.SECONDS).minus(Duration.ofHours(2));
        PrnAdministration saved = prnRepository.save(prnAdministration(at));

        PrnAdministrationBody body = prn.getPrnHistory(prescription.getId()).administrations().stream()
                .filter(a -> a.id().equals(saved.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(body.adverseReaction()).isEqualTo("Buồn nôn nhẹ");
        assertThat(body.note()).isEqualTo("Uống sau ăn");
        assertThat(body.recordedAfterDisconnect()).isTrue();
        assertThat(body.administeredBy()).isEqualTo("Điều dưỡng Lan");
    }

    @Test
    void FR_029_reviewOfPrn_identifiesRecordAndRecorder() {
        Instant at = time.now().truncatedTo(ChronoUnit.SECONDS).minus(Duration.ofHours(1));
        PrnAdministration saved = prnRepository.save(prnAdministration(at));
        DoseReview review = reviewRepository.save(new DoseReview(null, saved.getId(), null, null, null, time.now()));

        DoseReviewBody body = pendingReview(review.getId());

        assertThat(body.prnAdministrationId()).isEqualTo(saved.getId());
        assertThat(body.doseKey()).isNull();
        assertThat(body.residentId()).isEqualTo(residentId);
        assertThat(body.medicationName()).isEqualTo(medication.getName());
        assertThat(body.administeredAt()).isEqualTo(saved.getAdministeredAt());
        assertThat(body.recordedBy()).isEqualTo("Điều dưỡng Lan");
        assertThat(body.reviewedBy()).isNull();
    }

    @Test
    void FR_029_reviewOfDose_hasDoseKey() {
        Instant scheduled = time.now().truncatedTo(ChronoUnit.HOURS).minus(Duration.ofHours(1));
        MedicationDose dose = doseRepository.save(new MedicationDose(
                prescription.getId(),
                residentId,
                medication.getId(),
                scheduled,
                scheduled.minus(Duration.ofMinutes(30)),
                scheduled.plus(Duration.ofMinutes(30)),
                new BigDecimal("1"),
                "viên",
                MedicationSource.FAMILY_SENT,
                true,
                DoseStatus.ADMINISTERED,
                null,
                time.now()));
        DoseResult result = doseResultRepository.save(new DoseResult(
                dose.getId(),
                residentId,
                null,
                DoseResultType.ADMINISTERED,
                scheduled,
                null,
                nurse.accountId(),
                DoseAuthorityBasis.ASSIGNED,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                true,
                false,
                null,
                time.now()));
        DoseReview review = reviewRepository.save(new DoseReview(result.getId(), null, null, null, null, time.now()));

        DoseReviewBody body = pendingReview(review.getId());

        assertThat(body.doseKey()).isEqualTo(MedicationScheduleCalculator.doseKey(prescription.getId(), scheduled));
        assertThat(body.medicationName()).isEqualTo(medication.getName());
        assertThat(body.recordedBy()).isEqualTo("Điều dưỡng Lan");
    }

    private DoseReviewBody pendingReview(long reviewId) {
        return doses.listReviews(null).stream()
                .filter(r -> r.id().equals(reviewId))
                .findFirst()
                .orElseThrow();
    }

    private PrnAdministration prnAdministration(Instant at) {
        return new PrnAdministration(
                prescription.getId(),
                residentId,
                at,
                new BigDecimal("1"),
                "viên",
                "Đau đầu",
                nurse.accountId(),
                null,
                false,
                null,
                "Buồn nôn nhẹ",
                "Uống sau ăn",
                true,
                time.now());
    }
}
