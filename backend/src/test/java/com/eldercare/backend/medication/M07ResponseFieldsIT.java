package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.catalog.domain.Medication;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.PrnAdministration;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.DoseReviewRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.dose.persistence.PrnAdministrationRepository;
import com.eldercare.backend.medication.internal.family.domain.DelivererType;
import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationBatch;
import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationBatchStatus;
import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationPrescription;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationBatchRepository;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationPrescriptionRepository;
import com.eldercare.backend.medication.internal.prescription.domain.FrequencyType;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriberType;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.web.DoseController;
import com.eldercare.backend.medication.web.MedicationHandoverController;
import com.eldercare.backend.medication.web.PrnMedicationController;
import com.eldercare.backend.medication.web.response.DoseBodies.DoseReviewBody;
import com.eldercare.backend.medication.web.response.HandoverBodies.MedicationToBringBody;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
