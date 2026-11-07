package com.eldercare.backend.medication.internal.dose.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.offline.SyncOutcome;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.PrnAdministrationResponse;
import com.eldercare.backend.medication.internal.dose.domain.RecordPrnBody;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class T045_PrnOfflineHandlerTest {

    private final PrnMedicationService service = mock(PrnMedicationService.class);
    private final PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
    private final MedicationRepository medications = mock(MedicationRepository.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final PrnOfflineHandler handler = new PrnOfflineHandler(service, prescriptions, medications, user);

    @Test
    void T045_applyUsesDeviceTimeSyncingAccountAndMarksRecordedAfterDisconnect() {
        when(user.accountId()).thenReturn(71L);
        PrnAdministrationResponse saved = new PrnAdministrationResponse();
        saved.setId(81L);
        when(service.recordPrn(eq(31L), any(RecordPrnBody.class), eq(71L))).thenReturn(saved);
        Instant effectiveAt = Instant.parse("2026-10-29T01:15:00Z");

        String reference = handler.apply(content(), mock(SyncOutcome.class), effectiveAt);

        assertThat(reference).isEqualTo("prn:81");
        ArgumentCaptor<RecordPrnBody> request = ArgumentCaptor.forClass(RecordPrnBody.class);
        verify(service).recordPrn(eq(31L), request.capture(), eq(71L));
        assertThat(request.getValue().getOfflineDeviceTime()).isEqualTo(effectiveAt);
        assertThat(request.getValue().getAdministeredAt()).isEqualTo(Instant.parse("2026-10-29T01:00:00Z"));
        assertThat(request.getValue().isRecordedAfterDisconnect()).isTrue();
    }

    private PrnOfflineContent content() {
        return new PrnOfflineContent(
                31L,
                51L,
                new BigDecimal("1.00"),
                "Đau",
                null,
                Instant.parse("2026-10-29T01:00:00Z"),
                null,
                "Ghi ngoại tuyến");
    }
}
