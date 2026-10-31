package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.CurrentValue;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import com.eldercare.backend.medication.internal.dose.domain.DoseDetailView;
import com.eldercare.backend.medication.internal.dose.domain.DoseResult;
import com.eldercare.backend.medication.internal.dose.domain.DoseResultType;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.service.DoseResultOfflineHandler;
import com.eldercare.backend.medication.internal.dose.service.DoseViewService;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationBatchRepository;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationPrescriptionRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverDoseRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverRepository;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FR_030_CurrentDoseStatusTest {

    @Test
    void FR_030_currentStatusUsesLatestDoseResultCorrection() {
        DoseResult result = mock(DoseResult.class);
        when(result.getId()).thenReturn(501L);
        when(result.getResultType()).thenReturn(DoseResultType.MISSED);
        DoseResultRepository results = mock(DoseResultRepository.class);
        when(results.findByDoseId(101L)).thenReturn(Optional.of(result));
        CorrectionApi corrections = mock(CorrectionApi.class);
        when(corrections.current(DoseResultOfflineHandler.RECORD_TYPE, List.of(501L)))
                .thenReturn(Map.of(
                        501L, new CurrentValue(Map.of("resultType", DoseResultType.ADMINISTERED.name()), true, false)));
        DoseViewService views = new DoseViewService(
                results,
                mock(MedicationHandoverDoseRepository.class),
                mock(MedicationHandoverRepository.class),
                mock(FamilyMedicationBatchRepository.class),
                mock(FamilyMedicationPrescriptionRepository.class),
                mock(AccountApi.class),
                mock(ResidentApi.class),
                mock(BusinessTime.class),
                mock(ResidentLocationApi.class),
                mock(NursingShiftPort.class),
                corrections);
        DoseDetailView dose = mock(DoseDetailView.class);
        when(dose.doseId()).thenReturn(101L);
        when(dose.status()).thenReturn(DoseStatus.MISSED);
        when(dose.medicationSource()).thenReturn(MedicationSource.FACILITY);

        DoseViewService.DoseView view = views.view(dose);

        assertThat(view.currentStatus()).isEqualTo(DoseStatus.ADMINISTERED.name());
    }
}
