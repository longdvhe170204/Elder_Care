package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FR-050d, FR-049 (US11 #4): lựa chọn "chuyển bệnh viện" của cảnh báo nguy kịch thực hiện lệnh Chuyển viện của US5
 * trên sự cố đã liên kết với cảnh báo, kèm bản tóm tắt; lệnh Chuyển viện bị từ chối thì không ghi lựa chọn.
 */
class FR_050d_TransferChoiceIT extends EmergencyTestBase {
    @BeforeEach
    void transferSucceeds() {
        doAnswer(call -> new TransitionResult(
                        resident,
                        ResidentStatus.IN_RESIDENCE,
                        ResidentStatus.HOSPITALIZED,
                        StatusCommand.TRANSFER_OUT,
                        1L,
                        time.now(),
                        call.getArgument(4)))
                .when(statuses)
                .execute(eq(resident), eq(StatusCommand.TRANSFER_OUT), any(), any(), any(), any());
    }

    @Test
    void FR_050d_transferChoiceRunsTransferOutOnLinkedIncident() throws Exception {
        long alertId = flag(doctor).alertId();
        long incidentId = incident("FALL", "CRITICAL", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        jdbc.update("UPDATE alert SET incident_id = ? WHERE id = ?", incidentId, alertId);
        post(nurse, RECONFIRM, alertId, reconfirmation("KEPT")).andExpect(status().isCreated());

        post(doctor, CHOICE, alertId, choice(true)).andExpect(status().isCreated());

        verify(statuses).execute(eq(resident), eq(StatusCommand.TRANSFER_OUT), any(), any(), any(), any());
        assertThat(count("M05.CHOICE_EXECUTION", alertId)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind = 'M05.TRANSFER_SUMMARY' AND entity_id = ?",
                        Integer.class,
                        incidentId))
                .isEqualTo(1);
        // Tự gỡ dấu khi trạng thái đổi do bean tham gia lệnh trạng thái (đã kiểm ở BR_M05_15); `statuses` ở đây là bản
        // giả nên không kiểm lại.
    }

    @Test
    void FR_050d_transferChoiceNeedsTransferOutBodyAndLinkedIncident() throws Exception {
        long alertId = flag(doctor).alertId();
        post(nurse, RECONFIRM, alertId, reconfirmation("KEPT")).andExpect(status().isCreated());

        post(doctor, CHOICE, alertId, choice(false)).andExpect(status().isUnprocessableContent());
        post(doctor, CHOICE, alertId, choice(true))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONDITION_NOT_MET"));

        assertThat(count("M05.CHOICE_EXECUTION", alertId)).isZero();
    }

    private String choice(boolean withTransfer) {
        String transfer = withTransfer
                ? ",\"transferOut\":{\"transferUnit\":\"Bệnh viện X\",\"transferAt\":\"" + time.now()
                        + "\",\"companion\":\"Gia đình\",\"handoverStaffId\":" + nurse.staffId() + "}"
                : "";
        return "{\"choice\":\"TRANSFER_TO_HOSPITAL\"" + transfer + "}";
    }
}
