package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.medication.internal.dose.domain.DoseReview;
import com.eldercare.backend.medication.internal.dose.domain.DoseReviewStatus;
import com.eldercare.backend.medication.internal.dose.persistence.DoseReviewRepository;
import com.eldercare.backend.medication.internal.handover.domain.AbsenceIntervalType;
import com.eldercare.backend.medication.internal.handover.domain.MedicationAbsenceInterval;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandover;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandoverDose;
import com.eldercare.backend.medication.internal.handover.domain.MedicationHandoverItem;
import com.eldercare.backend.medication.internal.handover.domain.MedicationReturn;
import com.eldercare.backend.medication.internal.handover.domain.MedicationReturnItem;
import com.eldercare.backend.medication.internal.handover.domain.NoMedicationOrder;
import com.eldercare.backend.medication.internal.handover.domain.RecipientType;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationAbsenceIntervalRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverDoseRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverItemRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationReturnItemRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationReturnRepository;
import com.eldercare.backend.medication.internal.handover.persistence.NoMedicationOrderRepository;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionBasis;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionBasisCommand;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionBasisType;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionIssue;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionIssueType;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionBasisRepository;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionIssueRepository;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationCorrectionRequest;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationCorrectionStatus;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ReconciliationCorrectionRequestRepository;
import com.eldercare.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 006 T069: các loại đã gộp ({@code V006_96} → {@code V006_98}) ghi rồi đọc lại đúng qua lớp mặt tiền trên MySQL thật:
 * dòng JSON của lần giao, nhận lại, lệnh không mang thuốc trong {@code medication_handover}; khoảng vắng, chờ xem lại liều,
 * đề nghị đính chính phiếu trên {@code simple_record}; vấn đề đơn, căn cứ cơ sở kê trên {@code record_event}.
 */
@Transactional
class T069_ConsolidatedRecordsIT extends M07IntegrationTestBase {

    @Autowired
    EntityManager entityManager;

    @Autowired
    MedicationHandoverRepository handovers;

    @Autowired
    MedicationHandoverItemRepository handoverItems;

    @Autowired
    MedicationHandoverDoseRepository handoverDoses;

    @Autowired
    MedicationReturnRepository returns;

    @Autowired
    MedicationReturnItemRepository returnItems;

    @Autowired
    NoMedicationOrderRepository noMedicationOrders;

    @Autowired
    MedicationAbsenceIntervalRepository absences;

    @Autowired
    DoseReviewRepository reviews;

    @Autowired
    ReconciliationCorrectionRequestRepository correctionRequests;

    @Autowired
    PrescriptionIssueRepository issues;

    @Autowired
    PrescriptionBasisRepository bases;

    long residentId;
    Instant now;

    @BeforeEach
    void resident() {
        user.set(nurse.user());
        residentId = TestUsers.nextId();
        now = time.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private MedicationHandover handover(long tripId) {
        return handovers.saveAndFlush(new MedicationHandover(
                residentId, null, tripId, RecipientType.STAFF, null, "Người nhận", null, nurse.accountId(), now, now));
    }

    @Test
    void T069_handoverLinesRoundTripAndReturnLocksOnInsert() {
        long tripId = TestUsers.nextId();
        long doseId = TestUsers.nextId();
        MedicationHandover handover = handover(tripId);
        handoverItems.save(
                new MedicationHandoverItem(handover.getId(), 31L, new BigDecimal("2.000"), null, "FACILITY"));
        handoverItems.save(
                new MedicationHandoverItem(handover.getId(), 32L, new BigDecimal("1.500"), 9L, "FAMILY_SENT"));
        handoverDoses.save(new MedicationHandoverDose(handover.getId(), doseId));
        handovers.flush();

        assertThat(handoverItems.findByHandoverId(handover.getId()))
                .extracting(MedicationHandoverItem::getId, MedicationHandoverItem::getPrescriptionId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, 31L), org.assertj.core.groups.Tuple.tuple(2L, 32L));
        assertThat(handoverDoses.findByDoseId(doseId))
                .hasValueSatisfying(link -> assertThat(link.getHandoverId()).isEqualTo(handover.getId()));

        MedicationReturnItem item = new MedicationReturnItem(null, 31L, new BigDecimal("1"));
        item.assignId(1L);
        MedicationReturn saved = returns.saveAndFlush(
                new MedicationReturn(handover, nurse.accountId(), now, "Còn dư", List.of(item), now));
        handover.markReturned(now);
        handovers.saveAndFlush(handover);

        assertThat(returns.findByHandoverId(handover.getId())).isPresent();
        assertThat(returnItems.findByReturnId(saved.getId()))
                .singleElement()
                .satisfies(i -> assertThat(i.getQuantity()).isEqualByComparingTo("1"));
        assertThat(handovers.findById(saved.getId())).isEmpty();
        // Đọc lại lần giao đã khóa rồi flush: không được ghi lại cột JSON không đổi (trigger khóa dòng chặn UPDATE)
        entityManager.clear();
        assertThat(handovers.findById(handover.getId()).orElseThrow().getItems())
                .hasSize(2);
        returns.findById(saved.getId()).orElseThrow();
        handovers.flush();
        assertThatThrownBy(() -> {
                    handoverItems.save(
                            new MedicationHandoverItem(handover.getId(), 33L, BigDecimal.ONE, null, "FACILITY"));
                    handovers.flush();
                })
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void T069_noMedicationOrderIsUniquePerResidentAndTrip() {
        long tripId = TestUsers.nextId();
        noMedicationOrders.saveAndFlush(new NoMedicationOrder(residentId, tripId, "Không cần", nurse.accountId(), now));

        assertThat(noMedicationOrders.findByResidentIdAndTripId(residentId, tripId))
                .hasValueSatisfying(o -> assertThat(o.getLockedAt()).isNotNull());
        assertThatThrownBy(() -> noMedicationOrders.saveAndFlush(
                        new NoMedicationOrder(residentId, tripId, "Trùng", nurse.accountId(), now)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void T069_absenceIntervalOpenKeyFollowsTheInterval() {
        long tripId = TestUsers.nextId();
        MedicationAbsenceInterval interval = absences.save(new MedicationAbsenceInterval(
                residentId, AbsenceIntervalType.TRIP_WITH_MED, now, now.plusSeconds(3600), null, tripId, now));

        assertThat(absences.findOpenByResidentId(residentId)).hasValueSatisfying(open -> {
            assertThat(open.getId()).isEqualTo(interval.getId());
            assertThat(open.getFromTime()).isEqualTo(now);
            assertThat(open.getAbsenceType()).isEqualTo(AbsenceIntervalType.TRIP_WITH_MED);
        });
        assertThat(absences.findByTripId(tripId)).isPresent();

        MedicationAbsenceInterval open =
                absences.findOpenByResidentId(residentId).orElseThrow();
        open.close(now.plusSeconds(1800));
        absences.save(open);

        assertThat(absences.findOpenByResidentId(residentId)).isEmpty();
        assertThat(absences.findByTripId(tripId))
                .hasValueSatisfying(
                        closed -> assertThat(closed.getActualToTime()).isEqualTo(now.plusSeconds(1800)));
    }

    @Test
    void T069_doseReviewRoundTripByResultAndStatus() {
        long doseResultId = TestUsers.nextId();
        reviews.save(new DoseReview(doseResultId, null, 11L, "[9002]", "Ghi sau mất kết nối", now));

        DoseReview pending = reviews.findByDoseResultId(doseResultId).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(DoseReviewStatus.PENDING_REVIEW);
        assertThat(pending.getReviewerCandidate()).isEqualTo("[9002]");
        pending.accept(nurse.accountId(), "Đúng", now);
        reviews.save(pending);

        assertThat(reviews.findByStatus(DoseReviewStatus.ACCEPTED))
                .anySatisfy(r -> assertThat(r.getDoseResultId()).isEqualTo(doseResultId));
        assertThat(reviews.findById(pending.getId()))
                .hasValueSatisfying(r -> assertThat(r.getReviewerId()).isEqualTo(nurse.accountId()));
    }

    @Test
    void T069_correctionRequestRoundTrip() {
        long reconciliationId = TestUsers.nextId();
        ReconciliationCorrectionRequest request = correctionRequests.save(new ReconciliationCorrectionRequest(
                reconciliationId, "{\"note\": \"đúng\"}", "Sai dòng", nurse.accountId(), now, now));
        request.confirm(doctor.accountId(), now, 77L);
        correctionRequests.save(request);

        assertThat(correctionRequests.findById(request.getId())).hasValueSatisfying(r -> {
            assertThat(r.getStatus()).isEqualTo(ReconciliationCorrectionStatus.CONFIRMED);
            assertThat(r.getCorrectionId()).isEqualTo(77L);
            assertThat(r.getReconciliationId()).isEqualTo(reconciliationId);
        });
    }

    @Test
    void T069_issuesAndBasesAreEventsOfThePrescription() {
        long prescriptionId = TestUsers.nextId();
        issues.saveAll(List.of(new PrescriptionIssue(
                prescriptionId,
                null,
                null,
                PrescriptionIssueType.ALLERGY_CONFLICT,
                "ALLERGY",
                null,
                41L,
                "NANG",
                null,
                true,
                "Đã cân nhắc",
                doctor.accountId(),
                now,
                now)));
        bases.save(new PrescriptionBasis(
                prescriptionId,
                PrescriptionBasisCommand.EXTEND,
                "Bệnh viện A",
                "Bác sĩ B",
                LocalDate.of(2026, 10, 29),
                PrescriptionBasisType.NEW_PRESCRIPTION,
                null,
                null,
                null,
                55L,
                doctor.accountId(),
                now,
                now));

        assertThat(issues.findByPrescriptionId(prescriptionId)).singleElement().satisfies(i -> {
            assertThat(i.getIssueType()).isEqualTo(PrescriptionIssueType.ALLERGY_CONFLICT);
            assertThat(i.isManualReviewRequired()).isTrue();
            assertThat(i.getOccurredAt()).isEqualTo(now);
        });
        assertThat(bases.findByPrescriptionId(prescriptionId)).singleElement().satisfies(b -> {
            assertThat(b.getPrescribedDate()).isEqualTo(LocalDate.of(2026, 10, 29));
            assertThat(b.getDocumentFileId()).isEqualTo(55L);
        });
    }
}
