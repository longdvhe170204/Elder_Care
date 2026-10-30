package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.dto.NursingShiftView;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import com.eldercare.backend.medication.internal.domain.DoseStatus;
import com.eldercare.backend.medication.internal.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.persistence.FamilyMedicationBatchRepository;
import com.eldercare.backend.medication.internal.persistence.FamilyMedicationPrescriptionRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationHandoverDoseRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationHandoverRepository;
import com.eldercare.backend.medication.internal.service.DoseViewService;
import com.eldercare.backend.medication.internal.service.response.DoseDetailView;
import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FR_028_PendingHandoverConfirmationTest {

    private final Instant scheduled = Instant.parse("2026-10-28T00:30:00Z");
    private final Instant now = Instant.parse("2026-10-28T02:00:00Z");
    private NursingShiftPort shifts;
    private DoseViewService views;

    @BeforeEach
    void setUp() {
        ResidentLocationApi locations = mock(ResidentLocationApi.class);
        when(locations.at(201L, scheduled))
                .thenReturn(Optional.of(new LocationView(11L, "Floor", 11L, "Floor", 21L, "Room", 31L, "B1")));
        shifts = mock(NursingShiftPort.class);
        when(shifts.shiftAt(11L, scheduled))
                .thenReturn(Optional.of(new NursingShiftView(
                        91L, 11L, Instant.parse("2026-10-27T18:00:00Z"), Instant.parse("2026-10-28T01:00:00Z"))));
        when(shifts.inProgressShiftMember(77L, now)).thenReturn(false);
        BusinessTime time = mock(BusinessTime.class);
        when(time.now()).thenReturn(now);
        views = new DoseViewService(
                mock(DoseResultRepository.class),
                mock(MedicationHandoverDoseRepository.class),
                mock(MedicationHandoverRepository.class),
                mock(FamilyMedicationBatchRepository.class),
                mock(FamilyMedicationPrescriptionRepository.class),
                mock(AccountApi.class),
                mock(ResidentApi.class),
                time,
                locations,
                shifts,
                mock(CorrectionApi.class));
    }

    @Test
    void FR_028_openDoseFromEndedShiftWaitsForHandoverConfirmation() {
        when(shifts.handoverConfirmed(91L)).thenReturn(false);

        DoseViewService.DoseView view = views.view(openDose());

        assertThat(view.pendingHandoverConfirmation()).isTrue();
    }

    @Test
    void FR_028_confirmedHandoverClearsPendingFlag() {
        when(shifts.handoverConfirmed(91L)).thenReturn(true);

        DoseViewService.DoseView view = views.view(openDose());

        assertThat(view.pendingHandoverConfirmation()).isFalse();
    }

    private DoseDetailView openDose() {
        DoseDetailView dose = mock(DoseDetailView.class);
        when(dose.doseId()).thenReturn(101L);
        when(dose.prescriptionId()).thenReturn(10L);
        when(dose.residentId()).thenReturn(201L);
        when(dose.scheduledTime()).thenReturn(scheduled);
        when(dose.status()).thenReturn(DoseStatus.LATE);
        when(dose.medicationSource()).thenReturn(MedicationSource.FACILITY);
        when(dose.assignedNurseId()).thenReturn(77L);
        return dose;
    }
}
