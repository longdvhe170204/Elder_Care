package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.incident.internal.emergency.service.EmergencyCardService;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * BR-M05-14, FR-049, FR-050 (US5 #6, #7): Chuyển viện từ sự cố Mới hoặc Đang xử lý gọi lệnh Chuyển viện của 001 trong
 * cùng transaction với căn cứ là sự cố, rồi tạo bản tóm tắt; 001 từ chối thì không phần nào áp dụng và sự cố ghi lại
 * lần thử ({@code TRANSFER_REFUSED}).
 */
class BR_M05_14_TransferOutIT extends EmergencyTestBase {
    static final String TRANSFER = "/api/v1/incidents/{id}/transfer-out";

    @Autowired
    EmergencyCardService cards;

    @Test
    void BR_M05_14_transferCallsStatusCommandAndCreatesSummary() throws Exception {
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

    /** FR-048: thẻ thông tin khẩn cấp có ngày sinh từ hồ sơ 001 ({@code ResidentApi.dateOfBirth}, "Đổi api" 001). */
    @Test
    void FR_048_emergencyCardShowsDateOfBirth() {
        when(residents.dateOfBirth(resident)).thenReturn(Optional.of(LocalDate.of(1940, 3, 15)));

        assertThat(cards.build(resident, time.now()).identification().dateOfBirth())
                .isEqualTo(LocalDate.of(1940, 3, 15));
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

    /**
     * FR-050d, FR-049 (US11 #4; quickstart 007 Q55, lỗi B2; chủ dự án chốt 2026-11-12): lựa chọn "chuyển bệnh viện" trên cảnh
     * báo nguy kịch chưa có sự cố lập sự cố kèm lệnh với loại do người xử lý chọn, rồi Chuyển viện căn cứ sự cố đó; cảnh báo
     * vẫn mở để đóng sau.
     */
    @Test
    void FR_050d_transferChoiceOnCriticalAlert_recordsIncidentOfChosenTypeAndTransfers() throws Exception {
        activeWish(com.eldercare.backend.resident.api.dto.WishChoice.TRANSFER_TO_HOSPITAL);
        long alertId = flag(doctor).alertId();
        post(nurse, RECONFIRM, alertId, reconfirmation("KEPT")).andExpect(status().isCreated());
        accepted();

        post(doctor, CHOICE, alertId, "{\"choice\":\"TRANSFER_TO_HOSPITAL\",\"transferOut\":" + body() + "}")
                .andExpect(status().isUnprocessableContent());
        post(
                        doctor,
                        CHOICE,
                        alertId,
                        "{\"choice\":\"TRANSFER_TO_HOSPITAL\",\"incidentTypeId\":" + typeId("OTHER_HEALTH_CARE")
                                + ",\"transferOut\":" + body() + "}")
                .andExpect(status().isCreated());

        Long incidentId = jdbc.queryForObject("SELECT incident_id FROM alert WHERE id = ?", Long.class, alertId);
        assertThat(incidentId).isNotNull();
        assertThat(jdbc.queryForObject("SELECT type_code FROM incident WHERE id = ?", String.class, incidentId))
                .isEqualTo("OTHER_HEALTH_CARE");
        assertThat(jdbc.queryForObject("SELECT status FROM alert WHERE id = ?", String.class, alertId))
                .isNotEqualTo("CONVERTED_TO_INCIDENT");
        var basis = ArgumentCaptor.forClass(Basis.class);
        verify(statuses).execute(eq(resident), eq(StatusCommand.TRANSFER_OUT), any(), any(), any(), basis.capture());
        assertThat(basis.getValue().sourceRecordId()).isEqualTo(incidentId);
    }
}
