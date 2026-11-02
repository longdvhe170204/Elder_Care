package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.DoseReview;
import com.eldercare.backend.medication.internal.dose.domain.PrnAdministration;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.DoseReviewRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.dose.persistence.PrnAdministrationRepository;
import com.eldercare.backend.medication.internal.dose.service.DoseReviewService;
import com.eldercare.backend.medication.internal.governance.service.ActorNames;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DoseReviewServiceTest {

    @Test
    void SC_012_PRN_recorderCannotReviewTheirOwnRecord() {
        Instant now = Instant.parse("2026-10-30T08:00:00Z");
        DoseReviewRepository reviews = mock(DoseReviewRepository.class);
        PrnAdministrationRepository prnAdministrations = mock(PrnAdministrationRepository.class);
        DoseReview review = new DoseReview(null, 81L, 9L, null, null, now);
        PrnAdministration administration = mock(PrnAdministration.class);
        when(administration.getAdministeredById()).thenReturn(71L);
        when(reviews.findById(91L)).thenReturn(Optional.of(review));
        when(prnAdministrations.findById(81L)).thenReturn(Optional.of(administration));

        DoseReviewService service = new DoseReviewService(
                reviews,
                mock(DoseResultRepository.class),
                mock(MedicationDoseRepository.class),
                prnAdministrations,
                mock(PrescriptionRepository.class),
                mock(MedicationRepository.class),
                mock(ActorNames.class));

        assertThatThrownBy(() -> service.reviewDose(91L, true, null, 71L, now))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error -> org.assertj.core.api.Assertions.assertThat(error.errorCode())
                                .isEqualTo("SELF_REVIEW_NOT_ALLOWED"));
    }
}
