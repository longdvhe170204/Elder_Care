package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.incident.internal.emergency.service.EmergencyCardService;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * BR-M05-14, FR-049, FR-050 (US5 #6, #7): Chuyển viện từ sự cố Mới hoặc Đang xử lý gọi lệnh Chuyển viện của 001 trong
 * cùng transaction với căn cứ là sự cố, rồi tạo bản tóm tắt; 001 từ chối thì không phần nào áp dụng và sự cố ghi lại
 * lần thử ({@code TRANSFER_REFUSED}).
 */
class BR_45_TransferOutIT extends EmergencyTestBase {
    static final String TRANSFER = "/api/v1/incidents/{id}/transfer-out";

    @Autowired
    EmergencyCardService cards;

    @Test
    void BR_45_transferCallsStatusCommandAndCreatesSummary() throws Exception {
        long id = incident("FALL", "CRITICAL", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        accepted();

        post(doctor, TRANSFER, id, body())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receivingFacility").value("Bệnh viện X"))
                .andExpect(jsonPath("$.companion").value("Con gái M"))
                .andExpect(jsonPath("$.allergies[0].name").value("Penicillin"))
                .andExpect(jsonPath("$.currentMedications.length()").value(2))
                .andExpect(jsonPath("$.documentFileId").isNumber());

        var details = ArgumentCaptor.forClass(JsonNode.class);
        var basis = ArgumentCaptor.forClass(Basis.class);
        verify(statuses)
                .execute(
                        eq(resident), eq(StatusCommand.TRANSFER_OUT), any(), any(), details.capture(), basis.capture());
        assertThat(details.getValue().path("transferUnit").asString()).isEqualTo("Bệnh viện X");
        assertThat(details.getValue().path("handoverStaffId").asLong()).isEqualTo(nurse.staffId());
        assertThat(details.getValue().path("basis").path("incidentId").asLong()).isEqualTo(id);
        assertThat(basis.getValue().sourceRecordId()).isEqualTo(id);
        assertThat(count("M05.TRANSFER_SUMMARY", id)).isEqualTo(1);
    }

    void accepted() {
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

    String body() {
        return "{\"transferUnit\":\"Bệnh viện X\",\"transferAt\":\"" + time.now() + "\",\"companion\":\"Con gái M\","
                + "\"handoverStaffId\":" + nurse.staffId() + "}";
    }
}
