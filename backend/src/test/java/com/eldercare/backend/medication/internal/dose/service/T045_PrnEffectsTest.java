package com.eldercare.backend.medication.internal.dose.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.events.BilledSource;
import com.eldercare.backend.medication.api.events.MedicationBilledEvent;
import com.eldercare.backend.medication.internal.dose.domain.PrnAdministration;
import com.eldercare.backend.medication.internal.dose.domain.RecordPrnBody;
import com.eldercare.backend.medication.internal.dose.persistence.PrnAdministrationRepository;
import com.eldercare.backend.medication.internal.family.service.FamilyMedicationService;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ResidentMedicationLock;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class T045_PrnEffectsTest {

    private static final Instant ADMINISTERED_AT = Instant.parse("2026-10-29T02:00:00Z");

    private final PrnAdministrationRepository administrations = mock(PrnAdministrationRepository.class);
    private final PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
    private final ResidentMedicationLock lock = mock(ResidentMedicationLock.class);
    private final FamilyMedicationService familyMedication = mock(FamilyMedicationService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ResidentLocationApi locations = mock(ResidentLocationApi.class);
    private PrnMedicationService service;

    @BeforeEach
    void setUp() {
        service = new PrnMedicationService(
                administrations,
                prescriptions,
                lock,
                new BusinessTime(Clock.fixed(ADMINISTERED_AT, ZoneOffset.UTC)),
                familyMedication,
                events,
                locations);
        when(administrations.save(any(PrnAdministration.class))).thenAnswer(invocation -> {
            PrnAdministration saved = invocation.getArgument(0);
            setId(saved, 81L);
            return saved;
        });
        when(locations.at(41L, ADMINISTERED_AT))
                .thenReturn(Optional.of(new LocationView(null, null, 9L, "Tầng 9", 19L, "Phòng 19", 29L, "G29")));
    }

    @Test
    void FR_025a_facilityPrnPublishesBillingEventAndStoresOriginalFloor() {
        Prescription prescription = prescription(MedicationSource.FACILITY);
        when(prescriptions.findById(31L)).thenReturn(Optional.of(prescription));

        service.recordPrn(31L, body(), 71L);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue())
                .isEqualTo(new MedicationBilledEvent(
                        BilledSource.PRN_ADMINISTRATION,
                        81L,
                        41L,
                        51L,
                        new BigDecimal("1.50"),
                        "viên",
                        ADMINISTERED_AT));
        ArgumentCaptor<PrnAdministration> saved = ArgumentCaptor.forClass(PrnAdministration.class);
        verify(administrations).save(saved.capture());
        assertThat(saved.getValue().getFloorAreaId()).isEqualTo(9L);
    }

    @Test
    void Q_393_familyPrnDoesNotPublishBillingEvent() {
        Prescription prescription = prescription(MedicationSource.FAMILY_SENT);
        when(prescriptions.findById(31L)).thenReturn(Optional.of(prescription));
        when(familyMedication.hasUsableBatch(41L, 31L)).thenReturn(true);

        service.recordPrn(31L, body(), 71L);

        verify(events, never()).publishEvent(any());
    }

    private Prescription prescription(MedicationSource source) {
        Prescription prescription = mock(Prescription.class);
        when(prescription.getId()).thenReturn(31L);
        when(prescription.getResidentId()).thenReturn(41L);
        when(prescription.getMedicationId()).thenReturn(51L);
        when(prescription.getPrescriptionType()).thenReturn(PrescriptionType.PRN);
        when(prescription.getStatus()).thenReturn(PrescriptionStatus.ACTIVE);
        when(prescription.getDosage()).thenReturn(new BigDecimal("2.00"));
        when(prescription.getDosageUnit()).thenReturn("viên");
        when(prescription.getMedicationSource()).thenReturn(source);
        return prescription;
    }

    private RecordPrnBody body() {
        RecordPrnBody body = new RecordPrnBody();
        body.setAdministeredAt(ADMINISTERED_AT);
        body.setDosage(new BigDecimal("1.50"));
        body.setReason("Đau nhiều");
        return body;
    }

    private static void setId(PrnAdministration administration, Long id) {
        try {
            Field field = PrnAdministration.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(administration, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
