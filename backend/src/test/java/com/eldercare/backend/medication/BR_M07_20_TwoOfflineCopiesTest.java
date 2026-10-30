package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.events.MedicationAlertRequestedEvent;
import com.eldercare.backend.medication.internal.domain.DoseResult;
import com.eldercare.backend.medication.internal.domain.DoseResultType;
import com.eldercare.backend.medication.internal.domain.DoseReview;
import com.eldercare.backend.medication.internal.domain.MedicationDose;
import com.eldercare.backend.medication.internal.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.persistence.DoseReviewRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.service.DoseAdministrationService;
import com.eldercare.backend.medication.internal.service.DoseResultOfflineContent;
import com.eldercare.backend.medication.internal.service.DoseResultOfflineHandler;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class BR_M07_20_TwoOfflineCopiesTest {

    @Test
    void BR_M07_20_twoAdministeredCopiesCreateReviewAndMediumDuplicateAlert() {
        Fixture fixture = fixture(DoseResultType.ADMINISTERED);

        String source = fixture.handler.apply(content(DoseResultType.ADMINISTERED), null, fixture.now);

        assertThat(source).isEqualTo("dose_review:701");
        verify(fixture.reviews).save(any(DoseReview.class));
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(fixture.events).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOfSatisfying(MedicationAlertRequestedEvent.class, alert -> {
            assertThat(alert.alertType()).isEqualTo("POSSIBLE_DUPLICATE_DOSE");
            assertThat(alert.alertLevel()).isEqualTo("TRUNG_BINH");
        });
    }

    @Test
    void BR_M07_20_onlyTwoAdministeredCopiesCreateDuplicateDoseAlert() {
        Fixture fixture = fixture(DoseResultType.REFUSED);

        String source = fixture.handler.apply(content(DoseResultType.ADMINISTERED), null, fixture.now);

        assertThat(source).isEqualTo("dose_review:701");
        verify(fixture.reviews).save(any(DoseReview.class));
        verifyNoInteractions(fixture.events);
    }

    private static Fixture fixture(DoseResultType existingType) {
        Instant now = Instant.parse("2026-10-28T01:15:00Z");
        DoseAdministrationService administration = mock(DoseAdministrationService.class);
        when(administration.parseDoseKey("10-202610280800"))
                .thenReturn(new DoseAdministrationService.ParsedDoseKey(10L, Instant.parse("2026-10-28T01:00:00Z")));
        MedicationDose dose = mock(MedicationDose.class);
        when(dose.getId()).thenReturn(101L);
        when(dose.getFloorAreaId()).thenReturn(11L);
        MedicationDoseRepository doses = mock(MedicationDoseRepository.class);
        when(doses.findByPrescriptionIdAndScheduledTime(10L, Instant.parse("2026-10-28T01:00:00Z")))
                .thenReturn(Optional.of(dose));
        DoseResult existing = mock(DoseResult.class);
        when(existing.getId()).thenReturn(501L);
        when(existing.getResultType()).thenReturn(existingType);
        DoseResultRepository results = mock(DoseResultRepository.class);
        when(results.findByDoseId(101L)).thenReturn(Optional.of(existing));
        DoseReviewRepository reviews = mock(DoseReviewRepository.class);
        when(reviews.save(any(DoseReview.class))).thenAnswer(invocation -> {
            DoseReview review = invocation.getArgument(0);
            setId(review, 701L);
            return review;
        });
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        BusinessTime time = mock(BusinessTime.class);
        when(time.now()).thenReturn(now);
        DoseResultOfflineHandler handler = new DoseResultOfflineHandler(
                administration,
                doses,
                results,
                reviews,
                mock(PrescriptionRepository.class),
                mock(MedicationRepository.class),
                events,
                time);
        return new Fixture(handler, reviews, events, now);
    }

    private static DoseResultOfflineContent content(DoseResultType resultType) {
        return new DoseResultOfflineContent(
                "10-202610280800",
                201L,
                resultType,
                Instant.parse("2026-10-28T01:05:00Z"),
                null,
                null,
                null,
                null,
                null,
                77L);
    }

    private static void setId(Object entity, long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private record Fixture(
            DoseResultOfflineHandler handler,
            DoseReviewRepository reviews,
            ApplicationEventPublisher events,
            Instant now) {}
}
