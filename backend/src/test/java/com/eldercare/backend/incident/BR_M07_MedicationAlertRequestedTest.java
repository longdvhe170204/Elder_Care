package com.eldercare.backend.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import com.eldercare.backend.incident.internal.alert.service.MedicationAlertRequestListener;
import com.eldercare.backend.medication.api.events.MedicationAlertRequestedEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 006 → 007 (FR-026 (b), FR-032, research S-09): yêu cầu cảnh báo của thuốc thành cảnh báo theo người và nguồn (liều Bỏ
 * lỡ, xung đột dị ứng, phản ứng thuốc, nghi dùng trùng liều, chưa đối chiếu); phát lại không gộp thêm; phiếu đối chiếu
 * được xác nhận thì đóng cảnh báo "chưa đối chiếu" do Hệ thống.
 */
class BR_M07_MedicationAlertRequestedTest {

    private final AlertService alerts = mock(AlertService.class);
    private final ReminderMarkers markers = mock(ReminderMarkers.class);
    private final MedicationAlertRequestListener listener = new MedicationAlertRequestListener(alerts, markers);
    private final Instant at = Instant.parse("2026-11-08T03:00:00Z");

    private MedicationAlertRequestedEvent event(String type, String source) {
        return new MedicationAlertRequestedEvent(type, "TRUNG_BINH", 9L, source, "key:" + type, at, List.of());
    }

    @Test
    void BR_M07_20_duplicateDoseSourceIsADoseKeyAndMergesByPrescription() {
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(true);

        listener.onMedicationAlertRequested(event("POSSIBLE_DUPLICATE_DOSE", "doseKey:7-202611080800"));

        verify(alerts)
                .requestAlert(argThat(c -> c.typeLock().equals("THUOC:POSSIBLE_DUPLICATE_DOSE:7")
                        && c.sourceType().equals("DOSEKEY")
                        && c.sourceId() == 7));
    }

    @Test
    void FR_035_unreconciledAlertIsPerResident() {
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(true);

        listener.onMedicationAlertRequested(event("CHUA_DOI_CHIEU", "resident:9"));

        verify(alerts)
                .requestAlert(argThat(c -> c.typeLock().equals("THUOC:CHUA_DOI_CHIEU:9")
                        && c.sourceType().equals("RESIDENT")
                        && c.sourceId() == 9));
    }

    @Test
    void FR_032_redeliveredOrUnrelatedEventsDoNotCreateAlerts() {
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(false);
        listener.onMedicationAlertRequested(event("BO_LO_LIEU", "dose:77"));

        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(true);
        listener.onMedicationAlertRequested(event("DON_SAP_HET_HAN", "prescription:5"));
        listener.onMedicationAlertRequested(event("TU_CHOI_LIEN_TIEP", "dose:77"));

        verify(alerts, never()).requestAlert(any());
    }
}
