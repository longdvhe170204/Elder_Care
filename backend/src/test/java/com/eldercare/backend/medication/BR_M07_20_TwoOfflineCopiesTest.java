package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.AdministerDoseRequest;
import com.eldercare.backend.medication.api.events.MedicationAlertRequestedEvent;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.DoseDetailView;
import com.eldercare.backend.medication.internal.dose.domain.DoseResult;
import com.eldercare.backend.medication.internal.dose.domain.DoseResultType;
import com.eldercare.backend.medication.internal.dose.domain.DoseReview;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.DoseReviewRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.dose.service.DoseAdministrationService;
import com.eldercare.backend.medication.internal.dose.service.DoseResultOfflineContent;
import com.eldercare.backend.medication.internal.dose.service.DoseResultOfflineHandler;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
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

    @Test
    void FR_029_offlineDoseIsRecordedByTheSyncingAccountNotByContent() {
        Fixture fixture = fixture(null);
        DoseDetailView view = mock(DoseDetailView.class);
        when(view.doseId()).thenReturn(101L);
        when(fixture.administration.administerDose(any(), any(), any(), any(), any()))
                .thenReturn(view);

        String source = fixture.handler.apply(content(DoseResultType.ADMINISTERED), null, fixture.now);

        assertThat(source).isEqualTo("dose:101");
        verify(fixture.administration)
                .administerDose(eq("10-202610280800"), any(AdministerDoseRequest.class), eq(88L), isNull(), any());
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
        when(results.findByDoseId(101L)).thenReturn(existingType == null ? Optional.empty() : Optional.of(existing));
        DoseReviewRepository reviews = mock(DoseReviewRepository.class);
        when(reviews.save(any(DoseReview.class))).thenAnswer(invocation -> {
            DoseReview review = invocation.getArgument(0);
            setId(review, 701L);
            return review;
        });
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        BusinessTime time = mock(BusinessTime.class);
        when(time.now()).thenReturn(now);
        CurrentUser user = mock(CurrentUser.class);
        when(user.accountId()).thenReturn(88L);
        DoseResultOfflineHandler handler = new DoseResultOfflineHandler(
                administration,
                doses,
                results,
                reviews,
                mock(PrescriptionRepository.class),
                mock(MedicationRepository.class),
                events,
                time,
                user);
        return new Fixture(handler, administration, reviews, events, now);
    }

    private static DoseResultOfflineContent content(DoseResultType resultType) {
        return new DoseResultOfflineContent(
                "10-202610280800", 201L, resultType, Instant.parse("2026-10-28T01:05:00Z"), null, null, null, null);
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
            DoseAdministrationService administration,
            DoseReviewRepository reviews,
            ApplicationEventPublisher events,
            Instant now) {}
}
