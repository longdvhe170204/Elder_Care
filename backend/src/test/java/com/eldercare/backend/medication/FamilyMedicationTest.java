package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.family.domain.ConfirmBatchUseBody;
import com.eldercare.backend.medication.internal.family.domain.DelivererType;
import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationBatch;
import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationBatchStatus;
import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationPrescription;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationBatchRepository;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationPrescriptionRepository;
import com.eldercare.backend.medication.internal.family.service.FamilyMedicationService;
import com.eldercare.backend.medication.internal.prescription.domain.FrequencyType;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriberType;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ResidentMedicationLock;
import com.eldercare.backend.medication.web.request.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class FamilyMedicationTest {

    private FamilyMedicationBatchRepository batchRepo;
    private FamilyMedicationPrescriptionRepository batchPrescriptionRepo;
    private PrescriptionRepository prescriptionRepo;
    private MedicationRepository medicationRepo;
    private ResidentMedicationLock residentMedicationLock;
    private ApplicationEventPublisher events;

    private FamilyMedicationService familyMedicationService;

    @BeforeEach
    void setUp() {
        batchRepo = mock(FamilyMedicationBatchRepository.class);
        batchPrescriptionRepo = mock(FamilyMedicationPrescriptionRepository.class);
        prescriptionRepo = mock(PrescriptionRepository.class);
        medicationRepo = mock(MedicationRepository.class);
        residentMedicationLock = mock(ResidentMedicationLock.class);
        events = mock(ApplicationEventPublisher.class);

        familyMedicationService = new FamilyMedicationService(
                batchRepo,
                batchPrescriptionRepo,
                prescriptionRepo,
                medicationRepo,
                residentMedicationLock,
                events,
                new BusinessTime(Clock.systemUTC()));
    }

    private void setField(Object entity, String name, Object value) {
        try {
            Field f = entity.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(entity, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FamilyMedicationBatch createBatch(Long id, FamilyMedicationBatchStatus status) {
        FamilyMedicationBatch b = new FamilyMedicationBatch(
                100L,
                10L,
                "Donepezil",
                "Donepezil HCl",
                "5mg",
                new BigDecimal("30.0"),
                "viên",
                LocalDate.now().plusMonths(6),
                "Nguyên vẹn",
                true,
                null,
                DelivererType.FAMILY_MEMBER,
                200L,
                "Con trai",
                "Con",
                null,
                1L,
                Instant.now(),
                status,
                null,
                Instant.now());
        setField(b, "id", id);
        return b;
    }

    private Prescription createPrescription(Long id, Long residentId, Long medicationId) {
        Prescription p = new Prescription(
                residentId,
                medicationId,
                new BigDecimal("1.0"),
                "viên",
                "Oral",
                PrescriptionType.PERIODIC,
                FrequencyType.TIMED_SLOTS,
                "[\"08:00\"]",
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                LocalDate.now().plusMonths(1),
                MedicationSource.FAMILY_SENT,
                PrescriberType.INTERNAL,
                null,
                null,
                1L,
                null,
                null,
                null,
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                1L,
                Instant.now(),
                Set.of(medicationId),
                Instant.now());
        setField(p, "id", id);
        return p;
    }

    @Test
    @DisplayName("BR-M07-18: Xác nhận sử dụng thành công -> Được sử dụng và gắn đơn")
    void confirmUse_success() {
        FamilyMedicationBatch batch = createBatch(1L, FamilyMedicationBatchStatus.PENDING_RECONCILIATION);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));
        when(prescriptionRepo.findAllById(List.of(101L))).thenReturn(List.of(createPrescription(101L, 100L, 10L)));
        when(batchRepo.save(any(FamilyMedicationBatch.class))).thenReturn(batch);
        ConfirmBatchUseBody body = new ConfirmBatchUseBody();
        body.setPrescriptionIds(List.of(101L));
        body.setPackagingRecognizable(true);

        familyMedicationService.confirmUse(1L, body, 1L);

        assertThat(batch.getStatus()).isEqualTo(FamilyMedicationBatchStatus.IN_USE);
        verify(batchPrescriptionRepo).save(any(FamilyMedicationPrescription.class));
    }

    @Test
    @DisplayName("FR-041, FR-045 (Q-393): Ghi dùng hết -> Đã dùng hết, gỡ liên kết đơn")
    void markDepleted_inUse_becomesDepleted() {
        FamilyMedicationBatch batch = createBatch(1L, FamilyMedicationBatchStatus.IN_USE);
        when(batchRepo.findById(1L)).thenReturn(Optional.of(batch));
        FamilyMedicationPrescription link = new FamilyMedicationPrescription(1L, 101L, Instant.now());
        when(batchPrescriptionRepo.findByBatchIdAndBoundToIsNull(1L)).thenReturn(List.of(link));

        familyMedicationService.markDepleted(1L, 7L);

        assertThat(batch.getStatus()).isEqualTo(FamilyMedicationBatchStatus.DEPLETED);
        assertThat(batch.getClosedById()).isEqualTo(7L);
        assertThat(link.getBoundTo()).isNotNull();
    }

    @Test
    @DisplayName("FR-044 (Q-393): Đơn còn lô Được sử dụng chưa hết hạn thì xác nhận liều được; hết lô thì không")
    void hasUsableBatch_onlyInUseUnexpiredBound() {
        FamilyMedicationBatch inUse = createBatch(1L, FamilyMedicationBatchStatus.IN_USE);
        FamilyMedicationBatch expired = createBatch(2L, FamilyMedicationBatchStatus.IN_USE);
        setField(expired, "expiryDate", LocalDate.now().minusDays(1));
        when(batchPrescriptionRepo.findByPrescriptionIdAndBoundToIsNull(101L))
                .thenReturn(List.of(
                        new FamilyMedicationPrescription(1L, 101L, Instant.now()),
                        new FamilyMedicationPrescription(2L, 101L, Instant.now())));
        when(batchRepo.findByResidentIdAndStatus(100L, FamilyMedicationBatchStatus.IN_USE))
                .thenReturn(List.of(inUse, expired));

        assertThat(familyMedicationService.hasUsableBatch(100L, 101L)).isTrue();

        when(batchRepo.findByResidentIdAndStatus(100L, FamilyMedicationBatchStatus.IN_USE))
                .thenReturn(List.of(expired));
        assertThat(familyMedicationService.hasUsableBatch(100L, 101L)).isFalse();
    }
}
