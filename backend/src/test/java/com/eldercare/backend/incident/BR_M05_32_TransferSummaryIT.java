package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.NotifyContact;
import com.eldercare.backend.resident.api.dto.PreferredHospital;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.dto.TreatmentEscort;
import com.eldercare.backend.resident.api.dto.TreatmentWishView;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * BR-M05-32, FR-049, FR-050, FR-050h (US5 #8; quickstart C1 → C4; SC-021): màn lệnh hiện nguyện vọng khi điều trị;
 * người đi kèm bắt buộc; "cần hỏi trước" mà chưa liên lạc được thì cần lý do trừ sự cố Khẩn cấp; tệp đính kèm chuyển cho
 * 001; phiếu PDF lưu cùng lệnh, tải lại đúng file, in lại có dấu "Bản in lại" và ghi nhật ký mỗi lần in.
 */
class BR_M05_32_TransferSummaryIT extends EmergencyTestBase {
    private static final String TRANSFER = "/api/v1/incidents/{id}/transfer-out";

    @BeforeEach
    void treatmentWish() {
        when(wishes.currentTreatmentWish(resident))
                .thenReturn(Optional.of(new TreatmentWishView(
                        TestUsers.nextId(),
                        resident,
                        List.of(new PreferredHospital("Bệnh viện X", "Hà Nội")),
                        Optional.empty(),
                        true,
                        List.of(new NotifyContact(representativeId, "Trần Thị R", "0901000002")),
                        TreatmentEscort.FAMILY,
                        null,
                        time.now())));
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
    void FR_049_infoShowsTreatmentWish_noSuggestion() throws Exception {
        long id = incident("FALL", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        mvc.perform(as(doctor, MockMvcRequestBuilders.get("/api/v1/incidents/{id}/transfer-out/info", id)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.treatmentWish.preferredHospitals[0].name").value("Bệnh viện X"))
                .andExpect(jsonPath("$.mustAskFirst").value(true))
                .andExpect(jsonPath("$.isEmergency").value(false))
                .andExpect(jsonPath("$.primaryContact.fullName").value("Lê Văn Liên Hệ"));
    }

    @Test
    void FR_049_companionRequired_andReasonWhenNotAskedFirst() throws Exception {
        long id = incident("FALL", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        post(doctor, TRANSFER, id, body(null, false, null)).andExpect(status().isUnprocessableContent());
        post(doctor, TRANSFER, id, body("Gia đình", false, null)).andExpect(status().isUnprocessableContent());
        post(doctor, TRANSFER, id, body("Gia đình", false, "Gọi R không được, cần chuyển ngay"))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.treatmentWish.preferredHospitals[0].name").value("Bệnh viện X"));
    }

    @Test
    void FR_049_emergencyIncidentDoesNotNeedTheReason_andAttachmentsGoTo001() throws Exception {
        long id = incident("FALL", "CRITICAL", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        String body = body("Gia đình", false, null);
        body = body.substring(0, body.length() - 1) + ",\"attachmentIds\":[11,12]}";

        post(doctor, TRANSFER, id, body).andExpect(status().isCreated());

        var details = ArgumentCaptor.forClass(JsonNode.class);
        verify(statuses).execute(eq(resident), eq(StatusCommand.TRANSFER_OUT), any(), any(), details.capture(), any());
        assertThat(details.getValue().path("attachmentFileIds").toString()).isEqualTo("[11,12]");
    }

    @Test
    void FR_050h_pdfStoredWithCommand_downloadSameBytes_reprintStamped() throws Exception {
        long id = incident("FALL", "CRITICAL", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        var created = JsonMapper.builder()
                .build()
                .readTree(post(doctor, TRANSFER, id, body("Gia đình", true, null))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        long summary = created.path("id").asLong();

        mvc.perform(as(doctor, MockMvcRequestBuilders.get("/api/v1/transfer-summaries/{id}", summary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receivingFacility").value("Bệnh viện X"));
        byte[] first = document(summary);
        byte[] second = document(summary);
        assertThat(second).isEqualTo(first);
        String text = new PdfTextExtractor(new PdfReader(first)).getTextFromPage(1);
        assertThat(text.replaceAll("\\s+", " "))
                .contains("không thay giấy chuyển cơ sở khám bệnh, chữa bệnh")
                .contains("Bệnh viện X")
                .contains("Penicillin")
                .doesNotContain("Bản in lại");

        byte[] reprint = mvc.perform(
                        as(doctor, MockMvcRequestBuilders.post("/api/v1/transfer-summaries/{id}/reprint", summary)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        assertThat(new PdfTextExtractor(new PdfReader(reprint)).getTextFromPage(1))
                .contains("Bản in lại");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'TRANSFER_SUMMARY_PRINTED' AND entity_id = ?",
                        Integer.class,
                        Long.toString(summary)))
                .isEqualTo(2);
    }

    private byte[] document(long summary) throws Exception {
        return mvc.perform(as(doctor, MockMvcRequestBuilders.get("/api/v1/transfer-summaries/{id}/document", summary)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
    }

    private String body(String companion, boolean reached, String reason) {
        StringBuilder json = new StringBuilder("{\"transferUnit\":\"Bệnh viện X\",\"transferAt\":\"" + time.now()
                + "\",\"handoverStaffId\":" + nurse.staffId() + ",\"requiredContactReached\":" + reached);
        if (companion != null)
            json.append(",\"companion\":\"").append(companion).append('"');
        if (reason != null)
            json.append(",\"notAskedFirstReason\":\"").append(reason).append('"');
        return json.append('}').toString();
    }
}
